package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;

public class ForeldrepengerUttak {

    /**
     * Fra regler
     */
    private final List<ForeldrepengerUttakPeriode> perioder;

    /**
     * Behandlet manuelt av saksbehandler. Gjeldende hvis eksisterer
     */
    private final List<ForeldrepengerUttakPeriode> overstyrtePerioder;

    private final Map<StønadskontoType, Integer> stønadskontoBeregning;

    public ForeldrepengerUttak(List<ForeldrepengerUttakPeriode> perioder,
                               List<ForeldrepengerUttakPeriode> overstyrtePerioder,
                               Map<StønadskontoType, Integer> stønadskontoBeregning) {
        this.perioder = Objects.requireNonNull(perioder);
        this.overstyrtePerioder = Optional.ofNullable(overstyrtePerioder).orElse(List.of());
        this.stønadskontoBeregning = Optional.ofNullable(stønadskontoBeregning).orElse(Map.of());
    }

    public ForeldrepengerUttak(List<ForeldrepengerUttakPeriode> perioder) {
        this(perioder, List.of(), Map.of());
    }

    public Map<StønadskontoType, Integer> getStønadskontoBeregning() {
        return stønadskontoBeregning;
    }

    public List<ForeldrepengerUttakPeriode> getGjeldendePerioder() {
        if (!overstyrtePerioder.isEmpty()) {
            return sortByFom(overstyrtePerioder);
        }
        return getOpprinneligPerioder();
    }

    public List<ForeldrepengerUttakPeriode> getOpprinneligPerioder() {
        return sortByFom(perioder);
    }

    public LocalDate finnFørsteUttaksdato() {
        return getGjeldendePerioder().stream().map(p -> p.getTidsperiode().getFomDato()).min(LocalDate::compareTo).orElseThrow();
    }

    public Optional<LocalDate> finnFørsteUttaksdatoHvisFinnes() {
        return getGjeldendePerioder().stream().map(p -> p.getTidsperiode().getFomDato()).min(LocalDate::compareTo);
    }

    public LocalDate sistDagMedTrekkdager() {
        return getGjeldendePerioder().stream()
            .filter(p -> p.harTrekkdager() || p.isInnvilgetOpphold())
            .map(p -> p.getTidsperiode().getTomDato())
            .max(LocalDate::compareTo)
            .orElseThrow();
    }

    private List<ForeldrepengerUttakPeriode> sortByFom(List<ForeldrepengerUttakPeriode> perioder) {
        return perioder.stream().sorted(Comparator.comparing(ForeldrepengerUttakPeriode::getFom)).toList();
    }

    public boolean harUtbetaling() {
        return getGjeldendePerioder().stream().anyMatch(ForeldrepengerUttakPeriode::harUtbetaling);
    }

    public boolean harAvslagPgaMedlemskap() {
        return getGjeldendePerioder().stream().anyMatch(ForeldrepengerUttakPeriode::harAvslagPgaMedlemskap);
    }
}
