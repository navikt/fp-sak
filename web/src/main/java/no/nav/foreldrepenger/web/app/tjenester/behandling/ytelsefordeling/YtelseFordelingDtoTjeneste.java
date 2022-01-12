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
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
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
        if (ytelseFordelingAggregat.isPresent()) {
            var perioderUtenOmsorg = ytelseFordelingAggregat.get().getPerioderUtenOmsorg();
            var perioderAleneOmsorg = ytelseFordelingAggregat.get().getPerioderAleneOmsorg();
            var avklarteUttakDatoerOpt = ytelseFordelingAggregat.get().getAvklarteDatoer();
            var perioderAnnenforelderHarRett = ytelseFordelingAggregat.get().getPerioderAnnenforelderHarRett();

            if (perioderAleneOmsorg.isPresent()) {
                var periodeAleneOmsorgs = perioderAleneOmsorg.get().getPerioder();
                dtoBuilder.medAleneOmsorgPerioder(PeriodeKonverter.mapAleneOmsorgsperioder(periodeAleneOmsorgs));
            }
            if (perioderUtenOmsorg.isPresent()) {
                var periodeUtenOmsorgs = perioderUtenOmsorg.get().getPerioder();
                dtoBuilder.medIkkeOmsorgPerioder(PeriodeKonverter.mapUtenOmsorgperioder(periodeUtenOmsorgs));
            }
            if (avklarteUttakDatoerOpt.isPresent()) {
                dtoBuilder.medEndringsdato(avklarteUttakDatoerOpt.get().getGjeldendeEndringsdato());
            }
            leggTilFørsteUttaksdato(behandling, dtoBuilder);
            lagAnnenforelderHarRettDto(behandling, perioderAnnenforelderHarRett, dtoBuilder);
        }
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
        if (perioderAnnenforelderHarRett.isPresent()) {
            var periodeAnnenforelderHarRett = perioderAnnenforelderHarRett.get().getPerioder();
            var annenforelderHarRettDto = new AnnenforelderHarRettDto(begrunnelse, !periodeAnnenforelderHarRett.isEmpty(),
                PeriodeKonverter.mapAnnenforelderHarRettPerioder(periodeAnnenforelderHarRett), avklareUføretrygd);
            dtoBuilder.medAnnenforelderHarRett(annenforelderHarRettDto);
        } else {
            dtoBuilder.medAnnenforelderHarRett(new AnnenforelderHarRettDto(begrunnelse, null, null, avklareUføretrygd));
        }

    }

}
