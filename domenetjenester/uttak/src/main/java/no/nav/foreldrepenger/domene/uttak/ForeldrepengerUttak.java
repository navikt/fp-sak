package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class ForeldrepengerUttak implements Uttak {

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

    @Override
    public boolean altAvslått() {
        return getGjeldendePerioder().stream().allMatch(p -> PeriodeResultatType.AVSLÅTT.equals(p.getResultatType()));
    }

    @Override
    public Optional<LocalDate> opphørsdato() {
        var sistePeriode = getGjeldendePerioder().stream().max(Comparator.comparing(ForeldrepengerUttakPeriode::getTom));
        if (sistePeriode.filter(ForeldrepengerUttakPeriode::isOpphør).isEmpty()) {
            return Optional.empty();
        }
        var reversed = getGjeldendePerioder().stream().sorted(Comparator.comparing(ForeldrepengerUttakPeriode::getFom).reversed()).toList();

        // Fom dato i første periode av de siste sammenhengende periodene med opphør
        LocalDate opphørFom = null;
        for (var periode : reversed) {
            if (periode.isOpphør()) {
                opphørFom = periode.getFom();
            } else if (opphørFom != null) {
                return Optional.of(opphørFom);
            }
        }
        return Optional.ofNullable(opphørFom);
    }

    @Override
    public boolean harUlikUttaksplan(Uttak other) {
        var uttakresultatSammenligneMed = (ForeldrepengerUttak) other;
        var uttaksTL = lagTidslinjeFraUttaksPerioder(uttakresultatSammenligneMed.getGjeldendePerioder());
        var originalTL = lagTidslinjeFraUttaksPerioder(getGjeldendePerioder());
        if (uttaksTL.getLocalDateIntervals().size() != originalTL.getLocalDateIntervals().size()) {
            return true;
        }
        var kombinert = uttaksTL.combine(originalTL, ForeldrepengerUttak::fjernLikePerioder, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        return !kombinert.filterValue(Objects::nonNull).getLocalDateIntervals().isEmpty();
    }

    @Override
    public boolean harUlikKontoEllerMinsterett(Uttak other) {
        var uttakresultatSammenligneMed = (ForeldrepengerUttak) other;
        return !Objects.equals(getKontoDager(this), getKontoDager(uttakresultatSammenligneMed)) || !Objects.equals(getMinsterettDager(this),
            getMinsterettDager(uttakresultatSammenligneMed));
    }

    @Override
    public boolean harOpphørsUttakNyeInnvilgetePerioder(Uttak other) {
        var uttakresultatSammenligneMed = (ForeldrepengerUttak) other;
        var uttaksTL = lagTidslinjeFraUttaksPerioder(uttakresultatSammenligneMed.getGjeldendePerioder());
        var originalTL = lagTidslinjeFraUttaksPerioder(getGjeldendePerioder());
        return uttaksTL.combine(originalTL, ForeldrepengerUttak::fjernLikePerioder, LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .toSegments()
            .stream()
            .map(LocalDateSegment::getValue)
            .filter(Objects::nonNull)
            .map(WrapUttakPeriode::getP)
            .anyMatch(ForeldrepengerUttakPeriode::harAktivtUttak);
    }

    private static Integer getKontoDager(ForeldrepengerUttak uttak) {
        var u = uttak.getStønadskontoBeregning();
        return u.getOrDefault(StønadskontoType.FORELDREPENGER, 0) + u.getOrDefault(StønadskontoType.FELLESPERIODE, 0);
    }

    private static Integer getMinsterettDager(ForeldrepengerUttak uttak) {
        var u = uttak.getStønadskontoBeregning();
        return u.getOrDefault(StønadskontoType.BARE_FAR_RETT, 0) + u.getOrDefault(StønadskontoType.TETTE_SAKER_MOR, 0) + u.getOrDefault(
            StønadskontoType.TETTE_SAKER_FAR, 0);
    }

    private static LocalDateTimeline<WrapUttakPeriode> lagTidslinjeFraUttaksPerioder(List<ForeldrepengerUttakPeriode> uttaksPerioder) {
        return new LocalDateTimeline<>(uttaksPerioder.stream()
            .map(p -> new WrapUttakPeriode(p.getTidsperiode(), p))
            .map(w -> new LocalDateSegment<>(w.getI(), w))
            .toList()).compress(WrapUttakPeriode::erLikeNaboer, ForeldrepengerUttak::kombinerLikeNaboer);
    }

    private static LocalDateSegment<WrapUttakPeriode> kombinerLikeNaboer(LocalDateInterval i,
                                                                         LocalDateSegment<WrapUttakPeriode> lhs,
                                                                         LocalDateSegment<WrapUttakPeriode> rhs) {
        if (lhs == null) {
            return rhs;
        }
        if (rhs == null) {
            return lhs;
        }
        return new LocalDateSegment<>(i, new WrapUttakPeriode(i, lhs.getValue(), rhs.getValue()));
    }

    private static LocalDateSegment<WrapUttakPeriode> fjernLikePerioder(LocalDateInterval i,
                                                                        LocalDateSegment<WrapUttakPeriode> lhs,
                                                                        LocalDateSegment<WrapUttakPeriode> rhs) {
        if (lhs == null) {
            return rhs;
        }
        if (rhs == null) {
            return lhs;
        }
        // Kan ikke sammenligne splittede intervaller pga trekkdager - må være like for
        // å eliminere
        if (!Objects.equals(lhs.getValue().getI(), rhs.getValue().getI())) {
            return lhs;
        }
        if (lhs.getValue().erLikePerioderTrekkdager(rhs.getValue())) {
            return null;
        }
        return lhs;
    }

    private static class WrapUttakPeriode {
        private final LocalDateInterval i;
        private final ForeldrepengerUttakPeriode p;
        private final Map<ForeldrepengerUttakAktivitet, Trekkdager> t;

        WrapUttakPeriode(LocalDateInterval i, ForeldrepengerUttakPeriode p) {
            this.i = i;
            this.p = p;
            this.t = p.getAktiviteter()
                .stream()
                .collect(Collectors.groupingBy(ForeldrepengerUttakPeriodeAktivitet::getUttakAktivitet,
                    Collectors.reducing(Trekkdager.ZERO, ForeldrepengerUttakPeriodeAktivitet::getTrekkdager, Trekkdager::add)));
        }

        WrapUttakPeriode(LocalDateInterval i, WrapUttakPeriode p1, WrapUttakPeriode p2) {
            this.i = i;
            this.p = p1.getP();
            this.t = Stream.of(p1.getP().getAktiviteter(), p2.getP().getAktiviteter())
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(ForeldrepengerUttakPeriodeAktivitet::getUttakAktivitet,
                    Collectors.reducing(Trekkdager.ZERO, ForeldrepengerUttakPeriodeAktivitet::getTrekkdager, Trekkdager::add)));
        }

        public LocalDateInterval getI() {
            return i;
        }

        public ForeldrepengerUttakPeriode getP() {
            return p;
        }

        public boolean erLikeNaboer(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            var wrapUP = (WrapUttakPeriode) o;
            return p.erLikBortsettFraPeriode(wrapUP.getP());
        }

        public boolean erLikePerioderTrekkdager(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            var wrapUP = (WrapUttakPeriode) o;
            return p.erLikBortsettFraPeriode(wrapUP.getP()) && Objects.equals(t, wrapUP.t);
        }

    }
}
