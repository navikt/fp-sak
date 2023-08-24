package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

import java.time.LocalDate;
import java.util.Optional;

@ApplicationScoped
public class YtelseFordelingDtoTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private UføretrygdRepository uføretrygdRepository;
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    YtelseFordelingDtoTjeneste() {
        //CDI
    }

    @Inject
    public YtelseFordelingDtoTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                      FagsakRelasjonRepository fagsakRelasjonRepository,
                                      UføretrygdRepository uføretrygdRepository,
                                      ForeldrepengerUttakTjeneste uttakTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
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
            var førsteUttaksdato = finnFørsteUttaksdato(behandling);
            førsteUttaksdato.ifPresent(dtoBuilder::medFørsteUttaksdato);
            dtoBuilder.medØnskerJustertVedFødsel(yfa.getGjeldendeFordeling().ønskerJustertVedFødsel());
            dtoBuilder.medRettigheterAnnenforelder(lagAnnenforelderRettDto(behandling, yfa));
        });
        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak());
        fagsakRelasjon.ifPresent(fagsakRelasjon1 -> dtoBuilder.medGjeldendeDekningsgrad(fagsakRelasjon1.getGjeldendeDekningsgrad().getVerdi()));
        return Optional.of(dtoBuilder.build());
    }

    private RettigheterAnnenforelderDto lagAnnenforelderRettDto(Behandling behandling, YtelseFordelingAggregat yfa) {
        var uføregrunnlag = uføretrygdRepository.hentGrunnlag(behandling.getId());
        var avklareUføretrygd = yfa.getMorUføretrygdAvklaring() == null && uføregrunnlag.filter(UføretrygdGrunnlagEntitet::uavklartAnnenForelderMottarUføretrygd).isPresent();
        var avklareRettEØS = yfa.getAnnenForelderRettEØSAvklaring() == null && UttakOmsorgUtil.oppgittAnnenForelderTilknytningEØS(yfa);
        return new RettigheterAnnenforelderDto(yfa.getAnnenForelderRettAvklaring(),
            yfa.getAnnenForelderRettEØSAvklaring(), avklareRettEØS,
            yfa.getMorUføretrygdAvklaring(), avklareUføretrygd);
    }

    public Optional<LocalDate> finnFørsteUttaksdato(Behandling behandling) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        var førsteUttaksdato = ytelseFordelingAggregat.getAvklarteDatoer()
            .map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato);
        if (førsteUttaksdato.isPresent()) {
            return førsteUttaksdato;
        }
        return behandling.erRevurdering() ? finnFørsteUttaksdatoRevurdering(
            behandling) : finnFørsteUttaksdatoFørstegangsbehandling(behandling);
    }

    private Optional<LocalDate> finnFørsteUttaksdatoFørstegangsbehandling(Behandling behandling) {
        var oppgittePerioder = ytelseFordelingTjeneste.hentAggregat(behandling.getId())
            .getGjeldendeFordeling()
            .getPerioder();
        return oppgittePerioder.stream().map(OppgittPeriodeEntitet::getFom).min(LocalDate::compareTo);
    }

    private Optional<LocalDate> finnFørsteUttaksdatoRevurdering(Behandling behandling) {
        var revurderingId = behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Original behandling mangler på revurdering - skal ikke skje"));
        var uttak = uttakTjeneste.hentUttakHvisEksisterer(revurderingId);
        if (uttak.isEmpty() || uttak.get().getGjeldendePerioder().isEmpty()) {
            return finnFørsteUttaksdatoFørstegangsbehandling(behandling);
        }
        return uttak.get().finnFørsteUttaksdato();
    }

}
