package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAnnenforelderHarRettEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.rest.PeriodeKonverter;

@ApplicationScoped
public class YtelseFordelingDtoTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private UføretrygdRepository uføretrygdRepository;
    private FørsteUttaksdatoTjeneste førsteUttaksdatoTjeneste;

    YtelseFordelingDtoTjeneste() {
        //CDI
    }

    @Inject
    public YtelseFordelingDtoTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                      FagsakRelasjonRepository fagsakRelasjonRepository,
                                      UføretrygdRepository uføretrygdRepository,
                                      FørsteUttaksdatoTjeneste førsteUttaksdatoTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
        this.uføretrygdRepository = uføretrygdRepository;
        this.førsteUttaksdatoTjeneste = førsteUttaksdatoTjeneste;
    }

    public Optional<YtelseFordelingDto> mapFra(Behandling behandling) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandling.getId());
        var dtoBuilder = new YtelseFordelingDto.Builder();
        ytelseFordelingAggregat.ifPresent(yfa -> {
            yfa.getPerioderAleneOmsorg()
                .ifPresent(aleneomsorg -> dtoBuilder.medAleneOmsorgPerioder(PeriodeKonverter.mapAleneOmsorgsperioder(aleneomsorg.getPerioder())));
            dtoBuilder.medRettighetAleneomsorg(new RettighetDto(UttakOmsorgUtil.harAleneomsorg(yfa), yfa.getAleneomsorgAvklaring()));
            yfa.getPerioderUtenOmsorg()
                .ifPresent(uenOmsorg -> dtoBuilder.medIkkeOmsorgPerioder(PeriodeKonverter.mapUtenOmsorgperioder(uenOmsorg.getPerioder())));
            yfa.getAvklarteDatoer().ifPresent(avklarteUttakDatoer -> dtoBuilder.medEndringsdato(avklarteUttakDatoer.getGjeldendeEndringsdato()));
            leggTilFørsteUttaksdato(behandling, dtoBuilder);
            lagAnnenforelderHarRettDto(behandling, yfa.getPerioderAnnenforelderHarRett(), dtoBuilder);
            dtoBuilder.medRettigheterAnnenforelder(lagAnnenforelderRettDto(behandling, yfa));
        });
        fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak()).ifPresent(fagsakRelasjon1 -> dtoBuilder.medGjeldendeDekningsgrad(fagsakRelasjon1.getGjeldendeDekningsgrad().getVerdi()));
        return Optional.of(dtoBuilder.build());
    }

    private void leggTilFørsteUttaksdato(Behandling behandling, YtelseFordelingDto.Builder dtoBuilder) {
        var førsteUttaksdato = førsteUttaksdatoTjeneste.finnFørsteUttaksdato(behandling);
        if (førsteUttaksdato.isPresent()) {
            dtoBuilder.medFørsteUttaksdato(førsteUttaksdato.get());
        }
    }

    private void lagAnnenforelderHarRettDto(Behandling behandling, Optional<PerioderAnnenforelderHarRettEntitet> perioderAnnenforelderHarRett, YtelseFordelingDto.Builder dtoBuilder) {
        var begrunnelse = behandling.getAksjonspunkter().stream()
            .filter(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT))
            .findFirst().map(Aksjonspunkt::getBegrunnelse).orElse(null);
        var avklareUføretrygd = uføretrygdRepository.hentGrunnlag(behandling.getId())
            .filter(UføretrygdGrunnlagEntitet::uavklartAnnenForelderMottarUføretrygd)
            .isPresent();
        var avklartMottarUføretrygd = uføretrygdRepository.hentGrunnlag(behandling.getId())
            .map(UføretrygdGrunnlagEntitet::getUføretrygdOverstyrt).orElse(null);
        if (perioderAnnenforelderHarRett.isPresent()) {
            var periodeAnnenforelderHarRett = perioderAnnenforelderHarRett.get().getPerioder();
            var annenforelderHarRettDto = new AnnenforelderHarRettDto(begrunnelse, !periodeAnnenforelderHarRett.isEmpty(),
                PeriodeKonverter.mapAnnenforelderHarRettPerioder(periodeAnnenforelderHarRett), avklartMottarUføretrygd, avklareUføretrygd);
            dtoBuilder.medAnnenforelderHarRett(annenforelderHarRettDto);
        } else {
            dtoBuilder.medAnnenforelderHarRett(new AnnenforelderHarRettDto(begrunnelse, null, null, avklartMottarUføretrygd, avklareUføretrygd));
        }

    }

    private RettigheterAnnenforelderDto lagAnnenforelderRettDto(Behandling behandling, YtelseFordelingAggregat yfa) {
        var uføregrunnlag = uføretrygdRepository.hentGrunnlag(behandling.getId());
        var avklareUføretrygd = uføregrunnlag.filter(UføretrygdGrunnlagEntitet::uavklartAnnenForelderMottarUføretrygd).isPresent();
        var avklareStønadEØS = Boolean.TRUE.equals(yfa.getOppgittRettighet().getMorMottarStønadEØS());
        var avklartMottarUføretrygd = uføregrunnlag.map(UføretrygdGrunnlagEntitet::getUføretrygdOverstyrt).orElse(null);
        return new RettigheterAnnenforelderDto(new RettighetDto(UttakOmsorgUtil.harAnnenForelderRett(yfa, Optional.empty()), yfa.getAnnenForelderRettAvklaring()),
            new RettighetDto(UttakOmsorgUtil.morMottarUføretrygd(uføregrunnlag.orElse(null)), avklartMottarUføretrygd), avklareUføretrygd,
            new RettighetDto(UttakOmsorgUtil.morMottarForeldrepengerEØS(yfa), yfa.getMorStønadEØSAvklaring()), avklareStønadEØS);
    }

}
