package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class YtelseFordelingDtoTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private UføretrygdRepository uføretrygdRepository;
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    YtelseFordelingDtoTjeneste() {
        //CDI
    }

    @Inject
    public YtelseFordelingDtoTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                      DekningsgradTjeneste dekningsgradTjeneste,
                                      UføretrygdRepository uføretrygdRepository,
                                      ForeldrepengerUttakTjeneste uttakTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.uføretrygdRepository = uføretrygdRepository;
        this.uttakTjeneste = uttakTjeneste;
    }

    public Optional<YtelseFordelingDto> mapFra(Behandling behandling) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandling.getId());
        var dtoBuilder = new YtelseFordelingDto.Builder();
        ytelseFordelingAggregat.ifPresent(yfa -> {
            dtoBuilder.medBekreftetAleneomsorg(yfa.getAleneomsorgAvklaring());
            dtoBuilder.medOverstyrtOmsorg(yfa.getOverstyrtOmsorg());
            yfa.getAvklarteDatoer().ifPresent(avklarteUttakDatoer -> dtoBuilder.medEndringsdato(avklarteUttakDatoer.getGjeldendeEndringsdato()));
            dtoBuilder.medFørsteUttaksdato(finnFørsteUttaksdato(behandling));
            dtoBuilder.medØnskerJustertVedFødsel(yfa.getGjeldendeFordeling().ønskerJustertVedFødsel());
            dtoBuilder.medRettigheterAnnenforelder(lagAnnenforelderRettDto(behandling, yfa));
        });
        var fagsdekningsgradkRelasjon = dekningsgradTjeneste.finnGjeldendeDekningsgradHvisEksisterer(BehandlingReferanse.fra(behandling));
        fagsdekningsgradkRelasjon.ifPresent(d -> dtoBuilder.medGjeldendeDekningsgrad(d.getVerdi()));
        return Optional.of(dtoBuilder.build());
    }

    private RettigheterAnnenforelderDto lagAnnenforelderRettDto(Behandling behandling, YtelseFordelingAggregat yfa) {
        var uføregrunnlag = uføretrygdRepository.hentGrunnlag(behandling.getId());
        var avklareUføretrygd = yfa.getMorUføretrygdAvklaring() == null && uføregrunnlag.filter(UføretrygdGrunnlagEntitet::uavklartAnnenForelderMottarUføretrygd).isPresent();
        var avklareRettEØS = yfa.getAnnenForelderRettEØSAvklaring() == null && yfa.oppgittAnnenForelderTilknytningEØS();
        return new RettigheterAnnenforelderDto(yfa.getAnnenForelderRettAvklaring(),
            yfa.getAnnenForelderRettEØSAvklaring(), avklareRettEØS,
            yfa.getMorUføretrygdAvklaring(), avklareUføretrygd);
    }

    public LocalDate finnFørsteUttaksdato(Behandling behandling) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        var førsteUttaksdato = ytelseFordelingAggregat.getAvklarteDatoer()
            .map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato);
        return førsteUttaksdato.orElseGet(() -> behandling.erRevurdering() ? finnFørsteUttaksdatoRevurdering(
                behandling) : finnFørsteUttaksdatoFørstegangsbehandling(behandling));
    }

    private LocalDate finnFørsteUttaksdatoFørstegangsbehandling(Behandling behandling) {
        return ytelseFordelingTjeneste.hentAggregat(behandling.getId())
            .getGjeldendeFordeling()
            .finnFørsteUttaksdato().orElseThrow();
    }

    private LocalDate finnFørsteUttaksdatoRevurdering(Behandling behandling) {
        var originalBehandling = behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Original behandling mangler på revurdering - skal ikke skje"));
        var uttakOriginal = uttakTjeneste.hentUttakHvisEksisterer(originalBehandling);
        var førsteUttakOriginal = uttakOriginal.flatMap(ForeldrepengerUttak::finnFørsteUttaksdatoHvisFinnes);
        var førsteUttaksdatoTidligereBehandling = førsteUttakOriginal.orElse(Tid.TIDENES_ENDE);

        var førsteUttaksdatoSøkt = ytelseFordelingTjeneste.hentAggregat(behandling.getId())
            .getOppgittFordeling()
            .finnFørsteUttaksdato();

        return førsteUttaksdatoSøkt.filter(søktFom -> søktFom.isBefore(førsteUttaksdatoTidligereBehandling)).orElse(førsteUttaksdatoTidligereBehandling);
    }



}
