package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.weld.exceptions.UnsupportedOperationException;

import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class UttakResultatHolderFP implements UttakResultatHolder {

    private final Optional<ForeldrepengerUttak> uttakresultat;
    private BehandlingVedtak vedtak;

    public UttakResultatHolderFP(Optional<ForeldrepengerUttak> uttakresultat, BehandlingVedtak vedtak) {
        this.uttakresultat = uttakresultat;
        this.vedtak = vedtak;
    }

    @Override
    public LocalDate getSisteDagAvSistePeriode() {
        return uttakresultat.map(ForeldrepengerUttak::getGjeldendePerioder).orElse(Collections.emptyList()).stream()
                .filter(ForeldrepengerUttakPeriode::isInnvilget)
                .map(ForeldrepengerUttakPeriode::getTom)
                .max(Comparator.naturalOrder()).orElse(LocalDate.MIN);
    }

    @Override
    public LocalDate getFørsteDagAvFørstePeriode() {
        throw new UnsupportedOperationException("Not implemented"); // dummy
    }

    @Override
    public boolean eksistererUttakResultat() {
        return uttakresultat.isPresent();
    }

    private List<ForeldrepengerUttakPeriode> getGjeldendePerioder() {
        return uttakresultat.map(ForeldrepengerUttak::getGjeldendePerioder).orElse(List.of());
    }

    private Optional<ForeldrepengerUttakPeriode> finnSisteUttaksperiode() {
        return uttakresultat.map(ForeldrepengerUttak::getGjeldendePerioder).orElse(Collections.emptyList()).stream()
                .max(Comparator.comparing(ForeldrepengerUttakPeriode::getFom));
    }

    @Override
    public boolean erOpphør() {
        var opphørsAvslagÅrsaker = PeriodeResultatÅrsak.opphørsAvslagÅrsaker();
        return finnSisteUttaksperiode().map(ForeldrepengerUttakPeriode::getResultatÅrsak).map(opphørsAvslagÅrsaker::contains).orElse(false);
    }

    @Override
    public boolean harOpphørsUttakNyeInnvilgetePerioder(UttakResultatHolder other) {
        var uttakresultatSammenligneMed = (UttakResultatHolderFP) other;
        var uttaksTL = lagTidslinjeFraUttaksPerioder(uttakresultatSammenligneMed.getGjeldendePerioder());
        var originalTL = lagTidslinjeFraUttaksPerioder(getGjeldendePerioder());
        return uttaksTL.combine(originalTL, this::fjernLikePerioder, LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .toSegments().stream().map(LocalDateSegment::getValue).filter(Objects::nonNull)
            .map(WrapUttakPeriode::getP)
            .anyMatch(ForeldrepengerUttakPeriode::harAktivtUttak);
    }

    @Override
    public boolean harUlikUttaksplan(UttakResultatHolder other) {
        var uttakresultatSammenligneMed = (UttakResultatHolderFP) other;
        var uttaksTL = lagTidslinjeFraUttaksPerioder(uttakresultatSammenligneMed.getGjeldendePerioder());
        var originalTL = lagTidslinjeFraUttaksPerioder(getGjeldendePerioder());
        if (uttaksTL.getLocalDateIntervals().size() != originalTL.getLocalDateIntervals().size()) {
            return true;
        }
        var kombinert = uttaksTL.combine(originalTL, this::fjernLikePerioder, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        return !kombinert.filterValue(Objects::nonNull).getLocalDateIntervals().isEmpty();
    }

    @Override
    public boolean harUlikKontoEllerMinsterett(UttakResultatHolder other) {
        var uttakresultatSammenligneMed = (UttakResultatHolderFP) other;
        return !Objects.equals(getKontoDager(this), getKontoDager(uttakresultatSammenligneMed)) ||
            !Objects.equals(getMinsterettDager(this), getMinsterettDager(uttakresultatSammenligneMed));
    }

    private Integer getKontoDager(UttakResultatHolderFP uttakResultatHolder) {
        return uttakResultatHolder.uttakresultat.map(ForeldrepengerUttak::getStønadskontoBeregning)
            .map(u -> u.getOrDefault(StønadskontoType.FORELDREPENGER, 0) + u.getOrDefault(StønadskontoType.FELLESPERIODE, 0))
            .orElse(0);
    }

    private Integer getMinsterettDager(UttakResultatHolderFP uttakResultatHolder) {
        return uttakResultatHolder.uttakresultat.map(ForeldrepengerUttak::getStønadskontoBeregning)
            .map(u -> u.getOrDefault(StønadskontoType.BARE_FAR_RETT, 0) +
                u.getOrDefault(StønadskontoType.TETTE_SAKER_MOR, 0) +
                u.getOrDefault(StønadskontoType.TETTE_SAKER_FAR, 0))
            .orElse(0);
    }

    @Override
    public Optional<BehandlingVedtak> getBehandlingVedtak() {
        return Optional.ofNullable(vedtak);
    }


    private LocalDateTimeline<WrapUttakPeriode> lagTidslinjeFraUttaksPerioder(List<ForeldrepengerUttakPeriode> uttaksPerioder) {
        return new LocalDateTimeline<>(uttaksPerioder.stream()
                .map(p -> new WrapUttakPeriode(p.getTidsperiode(), p))
                .map(w -> new LocalDateSegment<>(w.getI(), w))
                .toList())
                        .compress(WrapUttakPeriode::erLikeNaboer, this::kombinerLikeNaboer);
    }

    private LocalDateSegment<WrapUttakPeriode> kombinerLikeNaboer(LocalDateInterval i, LocalDateSegment<WrapUttakPeriode> lhs,
            LocalDateSegment<WrapUttakPeriode> rhs) {
        if (lhs == null) {
            return rhs;
        }
        if (rhs == null) {
            return lhs;
        }
        return new LocalDateSegment<>(i, new WrapUttakPeriode(i, lhs.getValue(), rhs.getValue()));
    }

    private LocalDateSegment<WrapUttakPeriode> fjernLikePerioder(LocalDateInterval i, LocalDateSegment<WrapUttakPeriode> lhs,
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

    static class WrapUttakPeriode {
        private final LocalDateInterval i;
        private final ForeldrepengerUttakPeriode p;
        private final Map<ForeldrepengerUttakAktivitet, Trekkdager> t;

        WrapUttakPeriode(LocalDateInterval i, ForeldrepengerUttakPeriode p) {
            this.i = i;
            this.p = p;
            this.t = p.getAktiviteter().stream()
                    .collect(Collectors.groupingBy(ForeldrepengerUttakPeriodeAktivitet::getUttakAktivitet,
                            Collectors.reducing(Trekkdager.ZERO, ForeldrepengerUttakPeriodeAktivitet::getTrekkdager, Trekkdager::add)));
        }

        WrapUttakPeriode(LocalDateInterval i, WrapUttakPeriode p1, WrapUttakPeriode p2) {
            this.i = i;
            this.p = p1.getP();
            this.t = Stream.of(p1.getP().getAktiviteter(), p2.getP().getAktiviteter()).flatMap(Collection::stream)
                    .collect(Collectors.groupingBy(ForeldrepengerUttakPeriodeAktivitet::getUttakAktivitet,
                            Collectors.reducing(Trekkdager.ZERO, ForeldrepengerUttakPeriodeAktivitet::getTrekkdager, Trekkdager::add)));
        }

        public LocalDateInterval getI() {
            return i;
        }

        public ForeldrepengerUttakPeriode getP() {
            return p;
        }

        public Map<ForeldrepengerUttakAktivitet, Trekkdager> getT() {
            return t;
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
            return p.erLikBortsettFraPeriode(wrapUP.getP()) &&
                    Objects.equals(t, wrapUP.t);
        }

    }

}
