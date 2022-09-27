package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
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
        ytelseFordelingAggregat.ifPresent(yfa -> {
            dtoBuilder.medBekreftetAleneomsorg(yfa.getAleneomsorgAvklaring());
            yfa.getPerioderUtenOmsorg()
                .ifPresent(uenOmsorg -> dtoBuilder.medIkkeOmsorgPerioder(PeriodeKonverter.mapUtenOmsorgperioder(uenOmsorg.getPerioder())));
            yfa.getAvklarteDatoer().ifPresent(avklarteUttakDatoer -> dtoBuilder.medEndringsdato(avklarteUttakDatoer.getGjeldendeEndringsdato()));
            leggTilFørsteUttaksdato(behandling, dtoBuilder);
            dtoBuilder.medØnskerJustertVedFødsel(yfa.getGjeldendeSøknadsperioder().ønskerJustertVedFødsel());
            dtoBuilder.medRettigheterAnnenforelder(lagAnnenforelderRettDto(behandling, yfa));
        });
        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak());
        fagsakRelasjon.ifPresent(fagsakRelasjon1 -> dtoBuilder.medGjeldendeDekningsgrad(fagsakRelasjon1.getGjeldendeDekningsgrad().getVerdi()));
        return Optional.of(dtoBuilder.build());
    }

    private void leggTilFørsteUttaksdato(Behandling behandling, YtelseFordelingDto.Builder dtoBuilder) {
        var førsteUttaksdato = førsteUttaksdatoTjeneste.finnFørsteUttaksdato(behandling);
        førsteUttaksdato.ifPresent(dtoBuilder::medFørsteUttaksdato);
    }

    private RettigheterAnnenforelderDto lagAnnenforelderRettDto(Behandling behandling, YtelseFordelingAggregat yfa) {
        var uføregrunnlag = uføretrygdRepository.hentGrunnlag(behandling.getId());
        var avklareUføretrygd = uføregrunnlag.filter(UføretrygdGrunnlagEntitet::uavklartAnnenForelderMottarUføretrygd).isPresent();
        var avklareRettEØS = yfa.getOppgittRettighet().getAnnenForelderRettEØS();
        var avklartMottarUføretrygd = uføregrunnlag.map(UføretrygdGrunnlagEntitet::getUføretrygdOverstyrt).orElse(null);
        return new RettigheterAnnenforelderDto(yfa.getAnnenForelderRettAvklaring(),
            yfa.getAnnenForelderRettEØSAvklaring(), avklareRettEØS,
            avklartMottarUføretrygd, avklareUføretrygd);
    }

}
