package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ForeldrepengerUttak {

    /**
     * Fra regler
     */
    private final List<ForeldrepengerUttakPeriode> perioder;

    /**
     * Behandlet manuelt av saksbehandler. Gjeldende hvis eksisterer
     */
    private final List<ForeldrepengerUttakPeriode> overstyrtePerioder;

    public ForeldrepengerUttak(List<ForeldrepengerUttakPeriode> perioder, List<ForeldrepengerUttakPeriode> overstyrtePerioder) {
        this.perioder = Objects.requireNonNull(perioder);
        this.overstyrtePerioder = overstyrtePerioder == null ? List.of() : overstyrtePerioder;
    }

    public ForeldrepengerUttak(List<ForeldrepengerUttakPeriode> perioder) {
        this(perioder, List.of());
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
}
