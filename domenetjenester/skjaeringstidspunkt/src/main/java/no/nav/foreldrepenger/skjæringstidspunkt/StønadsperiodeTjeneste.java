package no.nav.foreldrepenger.skjæringstidspunkt;

import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.SØKNADSFRIST;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.konfig.Tid;

/*
 * Metode for å utlede stønadsperiode for et sakskompleks (sak+koblet sak)
 * - Svangerskapspenger: Perioder med utbetaling
 * - Foreldrepenger: Perioder med innvilget uttak/utsettelse, eller avslag/søknadsfrist
 *
 * Ekstrametoder for å finne sluttdato for enkeltsaker etter samme regler
 *
 */
@ApplicationScoped
public class StønadsperiodeTjeneste {

    private static final String YTELSE_IKKE_STØTTET = "Utviklerfeil: skal bare kalles for FP/SVP";
    private static final BigDecimal HUNDRE = new BigDecimal(100);

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository tilkjentRepository;
    private FpUttakRepository fpUttakRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    StønadsperiodeTjeneste() {
        // CDI
    }

    @Inject
    public StønadsperiodeTjeneste(FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                  BehandlingRepository behandlingRepository,
                                  FpUttakRepository fpUttakRepository,
                                  BeregningsresultatRepository tilkjentRepository,
                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.tilkjentRepository = tilkjentRepository;
        this.fpUttakRepository = fpUttakRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public Optional<LocalDate> stønadsperiodeStartdato(Fagsak fagsak) {
        return  behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .flatMap(this::stønadsperiodeStartdato);
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

    public Optional<LocalDateInterval> utbetalingsperiodeEnkeltSak(Fagsak fagsak) {
        return  behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .flatMap(this::stønadsperiodeEnkeltSakBR);
    }

    public LocalDateTimeline<Boolean> utbetalingsTidslinjeEnkeltSak(Fagsak fagsak) {
        return  behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .map(this::utbetalingsTidslinjeEnkeltSak).orElse(LocalDateTimeline.empty());
    }

    public LocalDateTimeline<Boolean> utbetalingsTidslinjeEnkeltSak(Behandling behandling) {
        return tilkjentRepository.hentUtbetBeregningsresultat(behandling.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
            .filter(p -> p.getDagsats() > 0)
            .map(p -> new LocalDateSegment<>(p.getBeregningsresultatPeriodeFom(), p.getBeregningsresultatPeriodeTom(), Boolean.TRUE))
            .collect(Collectors.collectingAndThen(Collectors.toList(), s -> new LocalDateTimeline<>(s, StandardCombinators::alwaysTrueForMatch)));
    }

    public boolean fullUtbetalingSisteUtbetalingsperiode(Fagsak fagsak) {
        return  behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .map(this::erFullUtbetalingSistePeriode).orElse(false);
    }

    public Optional<LocalDate> stønadsperiodeStartdatoEnkeltSak(Fagsak fagsak) {
        var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
        return switch (fagsak.getYtelseType()) {
            case FORELDREPENGER -> behandling.flatMap(this::stønadsperiodeStartdatoFraBehandling);
            case SVANGERSKAPSPENGER -> behandling.flatMap(this::stønadsperiodeStartdatoEnkeltSakBR);
            default -> throw new IllegalArgumentException(YTELSE_IKKE_STØTTET);
        };
    }

    public Optional<LocalDate> stønadsperiodeSluttdatoEnkeltSak(Fagsak fagsak) {
        var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
        return switch (fagsak.getYtelseType()) {
            case FORELDREPENGER -> behandling.flatMap(this::stønadsperiodeSluttdatoFraBehandling);
            case SVANGERSKAPSPENGER -> behandling.flatMap(this::stønadsperiodeSluttdatoEnkeltSakBR);
            default -> throw new IllegalArgumentException(YTELSE_IKKE_STØTTET);
        };
    }

    private Optional<LocalDate> stønadsperiodeStartdatoUR(Behandling behandling) {
        var startdato = stønadsperiodeStartdatoFraBehandling(behandling);
        var annenpartStartdato = vedtattBehandlingRelatertFagsak(behandling.getFagsakId())
            .flatMap(this::stønadsperiodeStartdatoFraBehandling);
        return startdato.filter(s -> s.isBefore(annenpartStartdato.orElse(Tid.TIDENES_ENDE))).or(() -> annenpartStartdato);
    }

    private Optional<LocalDateInterval> stønadsperiodeUR(Behandling behandling) {
        var brukstartdato = stønadsperiodeStartdatoUR(behandling);
        if (brukstartdato.isEmpty()) return Optional.empty();
        var sluttdato = stønadsperiodeSluttdatoFraBehandling(behandling);
        var annenpartSluttdato = vedtattBehandlingRelatertFagsak(behandling.getFagsakId())
            .flatMap(this::stønadsperiodeSluttdatoFraBehandling);
        var bruksluttdato = sluttdato.filter(s -> s.isAfter(annenpartSluttdato.orElse(Tid.TIDENES_BEGYNNELSE))).or(() -> annenpartSluttdato);
        return brukstartdato.map(s -> new LocalDateInterval(s, bruksluttdato.orElse(Tid.TIDENES_ENDE)));
    }

    private Optional<LocalDate> stønadsperiodeStartdatoFraBehandling(Behandling behandling) {
        if (RelasjonsRolleType.erMor(behandling.getRelasjonsRolleType())) {
            var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
            if (stp.gjelderFødsel()) {
                return stp.getSkjæringstidspunktHvisUtledet().map(VirkedagUtil::fomVirkedag);
            }
        }
        return fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())
            .map(UttakResultatEntitet::getGjeldendePerioder)
            .map(UttakResultatPerioderEntitet::getPerioder)
            .flatMap(StønadsperiodeTjeneste::finnFørsteStønadsdatoFraUttakResultat);

    }

    private Optional<LocalDate> stønadsperiodeSluttdatoFraBehandling(Behandling behandling) {
        return fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())
            .map(UttakResultatEntitet::getGjeldendePerioder).map(UttakResultatPerioderEntitet::getPerioder)
            .flatMap(StønadsperiodeTjeneste::finnSisteStønadsdatoFraUttakResultat);
    }


    private Optional<Behandling> vedtattBehandlingRelatertFagsak(Long fagsakId) {
        return fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsakId)
            .flatMap(r -> r.getRelatertFagsakFraId(fagsakId)).map(Fagsak::getId)
            .flatMap(behandlingRepository::finnSisteAvsluttedeIkkeHenlagteBehandling);
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

    public Optional<LocalDate> stønadsperiodeStartdatoEnkeltSakBR(Behandling behandling) {
        return tilkjentRepository.hentUtbetBeregningsresultat(behandling.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
            .filter(p -> p.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(Comparator.naturalOrder());
    }

    private Optional<LocalDate> stønadsperiodeSluttdatoEnkeltSakBR(Behandling behandling) {
        return tilkjentRepository.hentUtbetBeregningsresultat(behandling.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
            .filter(p -> p.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
            .max(Comparator.naturalOrder());
    }

    private Optional<LocalDateInterval> stønadsperiodeEnkeltSakBR(Behandling behandling) {
        var start = stønadsperiodeStartdatoEnkeltSakBR(behandling);
        if (start.isEmpty()) return Optional.empty();
        var slutt = stønadsperiodeSluttdatoEnkeltSakBR(behandling);
        return start.map(s -> new LocalDateInterval(s, slutt.orElse(Tid.TIDENES_ENDE)));
    }

    private boolean erFullUtbetalingSistePeriode(Behandling behandling) {
        return tilkjentRepository.hentUtbetBeregningsresultat(behandling.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
            .filter(p -> p.getDagsats() > 0)
            .max(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom))
            .map(BeregningsresultatPeriode::getKalkulertUtbetalingsgrad)
            .filter(ug -> ug.compareTo(HUNDRE) >= 0)
            .isPresent();
    }

}
