package no.nav.foreldrepenger.skjæringstidspunkt;

import static no.nav.foreldrepenger.behandlingslager.uttak.fp.IkkeOppfyltÅrsak.SØKNADSFRIST;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.konfig.Tid;

/*
 * Metode for å vurdere om en behandling skal vurderes etter nye eller gamle regler. Sjekker evt koblet fagsak.
 */
@ApplicationScoped
public class StønadsperiodeTjeneste {

    private static final String YTELSE_IKKE_STØTTET = "Utviklerfeil: skal bare kalles for FP/SVP";

    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository tilkjentRepository;
    private FpUttakRepository fpUttakRepository;

    StønadsperiodeTjeneste() {
        // CDI
    }

    @Inject
    public StønadsperiodeTjeneste(FagsakRelasjonRepository fagsakRelasjonRepository,
                                  BehandlingRepository behandlingRepository,
                                  FpUttakRepository fpUttakRepository,
                                  BeregningsresultatRepository tilkjentRepository) {
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
        this.behandlingRepository = behandlingRepository;
        this.tilkjentRepository = tilkjentRepository;
        this.fpUttakRepository = fpUttakRepository;
    }

    public Optional<LocalDate> stønadsperiodeStartdato(Fagsak fagsak) {
        return  behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .flatMap(this::stønadsperiodeStartdato);
    }

    public Optional<LocalDateInterval> stønadsperiode(Fagsak fagsak) {
        return  behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .flatMap(this::stønadsperiode);
    }

    public Optional<LocalDate> stønadsperiodeStartdato(Behandling behandling) {
        return switch (behandling.getFagsakYtelseType()) {
            case FORELDREPENGER -> stønadsperiodeStartdatoUR(behandling);
            case SVANGERSKAPSPENGER -> stønadsperiodeStartdatoEnkeltSakBR(behandling);
            default -> throw new IllegalArgumentException(YTELSE_IKKE_STØTTET);
        };
    }

    public Optional<LocalDateInterval> stønadsperiode(Behandling behandling) {
        return switch (behandling.getFagsakYtelseType()) {
            case FORELDREPENGER -> stønadsperiodeUR(behandling);
            case SVANGERSKAPSPENGER -> stønadsperiodeEnkeltSakBR(behandling);
            default -> throw new IllegalArgumentException(YTELSE_IKKE_STØTTET);
        };
    }

    private Optional<LocalDate> stønadsperiodeStartdatoUR(Behandling behandling) {
        var startdato = stønadsperiodeStartdatoFraBehandling(behandling.getId());
        var annenpartStartdato = vedtattBehandlingRelatertFagsak(behandling.getFagsakId())
            .flatMap(this::stønadsperiodeStartdatoFraBehandling);
        return startdato.filter(s -> s.isBefore(annenpartStartdato.orElse(Tid.TIDENES_ENDE))).or(() -> annenpartStartdato);
    }

    private Optional<LocalDateInterval> stønadsperiodeUR(Behandling behandling) {
        var brukstartdato = stønadsperiodeStartdatoUR(behandling);
        if (brukstartdato.isEmpty()) return Optional.empty();
        var sluttdato = stønadsperiodeSluttdato(behandling.getId());
        var annenpartSluttdato = vedtattBehandlingRelatertFagsak(behandling.getFagsakId())
            .flatMap(this::stønadsperiodeSluttdato);
        var bruksluttdato = sluttdato.filter(s -> s.isAfter(annenpartSluttdato.orElse(Tid.TIDENES_BEGYNNELSE))).or(() -> annenpartSluttdato);
        return brukstartdato.map(s -> new LocalDateInterval(s, bruksluttdato.orElse(Tid.TIDENES_ENDE)));
    }

    private Optional<LocalDate> stønadsperiodeStartdatoFraBehandling(Long behandlingId) {
        return fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId)
            .map(UttakResultatEntitet::getGjeldendePerioder).map(UttakResultatPerioderEntitet::getPerioder)
            .flatMap(StønadsperiodeTjeneste::finnFørsteStønadsdatoFraUttakResultat);
    }

    private Optional<LocalDate> stønadsperiodeSluttdato(Long behandlingId) {
        return fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId)
            .map(UttakResultatEntitet::getGjeldendePerioder).map(UttakResultatPerioderEntitet::getPerioder)
            .flatMap(StønadsperiodeTjeneste::finnSisteStønadsdatoFraUttakResultat);
    }


    private Optional<Long> vedtattBehandlingRelatertFagsak(Long fagsakId) {
        return fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsakId)
            .flatMap(r -> r.getRelatertFagsakFraId(fagsakId)).map(Fagsak::getId)
            .flatMap(behandlingRepository::finnSisteAvsluttedeIkkeHenlagteBehandling).map(Behandling::getId);
    }

    private static Optional<LocalDate> finnFørsteStønadsdatoFraUttakResultat(List<UttakResultatPeriodeEntitet> perioder) {
        return perioder.stream()
            .filter(it -> it.isInnvilget() || SØKNADSFRIST.equals(it.getResultatÅrsak()))
            .map(UttakResultatPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder());
    }

    private static Optional<LocalDate> finnSisteStønadsdatoFraUttakResultat(List<UttakResultatPeriodeEntitet> perioder) {
        return perioder.stream()
            .filter(it -> it.isInnvilget() || SØKNADSFRIST.equals(it.getResultatÅrsak()))
            .map(UttakResultatPeriodeEntitet::getTom)
            .max(Comparator.naturalOrder());
    }

    private Optional<LocalDate> stønadsperiodeStartdatoEnkeltSakBR(Behandling behandling) {
        return tilkjentRepository.hentUtbetBeregningsresultat(behandling.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
            .filter(p -> p.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(Comparator.naturalOrder());
    }

    private Optional<LocalDateInterval> stønadsperiodeEnkeltSakBR(Behandling behandling) {
        var start = stønadsperiodeStartdatoEnkeltSakBR(behandling);
        if (start.isEmpty()) return Optional.empty();
        var slutt = tilkjentRepository.hentUtbetBeregningsresultat(behandling.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
            .filter(p -> p.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
            .max(Comparator.naturalOrder());
        return start.map(s -> new LocalDateInterval(s, slutt.orElse(Tid.TIDENES_ENDE)));
    }

}
