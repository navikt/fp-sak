package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;


public class UttakResultatHolderImpl implements UttakResultatHolder {

    private static final Logger LOG = LoggerFactory.getLogger(UttakResultatHolderImpl.class);

    private final Optional<ForeldrepengerUttak> uttakresultat;
    private BehandlingVedtak vedtak;


    public UttakResultatHolderImpl(Optional<ForeldrepengerUttak> uttakresultat, BehandlingVedtak vedtak) {
        this.uttakresultat = uttakresultat;
        this.vedtak = vedtak;
    }

    @Override
    public Object getUttakResultat() {
        return uttakresultat.orElse(null);
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
        throw new UnsupportedOperationException("Not implemented");  // dummy
    }

    @Override
    public boolean eksistererUttakResultat() {
        return uttakresultat.isPresent();
    }

    @Override
    public List<ForeldrepengerUttakPeriode> getGjeldendePerioder() {
        if (uttakresultat.isEmpty()) {
            return List.of();
        }
        return uttakresultat.get().getGjeldendePerioder();
    }

    private Optional<ForeldrepengerUttakPeriode> finnSisteUttaksperiode() {
        return uttakresultat.map(ForeldrepengerUttak::getGjeldendePerioder).orElse(Collections.emptyList()).stream()
            .max(Comparator.comparing(ForeldrepengerUttakPeriode::getFom));
    }

    @Override
    public boolean kontrollerErSisteUttakAvslåttMedÅrsak() {
        Set<PeriodeResultatÅrsak> opphørsAvslagÅrsaker = IkkeOppfyltÅrsak.opphørsAvslagÅrsaker();
        return finnSisteUttaksperiode().map(ForeldrepengerUttakPeriode::getResultatÅrsak).map(opphørsAvslagÅrsaker::contains).orElse(false);
    }

    @Override
    public boolean vurderOmErEndringIUttak(UttakResultatHolder uttakresultatSammenligneMed){
        List<ForeldrepengerUttakPeriode> uttaksPerioderTP = uttakresultatSammenligneMed.getGjeldendePerioder();
        List<ForeldrepengerUttakPeriode> originaleUttaksPerioderTP = getGjeldendePerioder();
        var gammelImpl = !erUttakresultatperiodeneLike(uttaksPerioderTP, originaleUttaksPerioderTP);
        nySammenlignLogg(uttaksPerioderTP, originaleUttaksPerioderTP, gammelImpl);
        return gammelImpl;
    }

    @Override
    public Optional<BehandlingVedtak> getBehandlingVedtak() {
        return Optional.ofNullable(vedtak);
    }

    private boolean erUttakresultatperiodeneLike(List<ForeldrepengerUttakPeriode> listeMedPerioder1, List<ForeldrepengerUttakPeriode> listeMedPerioder2) {
        // Sjekk på Ny/fjernet
        if (listeMedPerioder1.size() != listeMedPerioder2.size()) {
            LOG.info("BEHRES avvik antall perioder");
            return false;
        }
        int antallPerioder = listeMedPerioder1.size();
        for (int i = 0; i < antallPerioder; i++) {
            if (!erLikPeriode(listeMedPerioder1.get(i), listeMedPerioder2.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean erLikPeriode(ForeldrepengerUttakPeriode p1, ForeldrepengerUttakPeriode p2) {
        if (p1.getAktiviteter().size() != p2.getAktiviteter().size()) {
            LOG.info("BEHRES avvik antall aktiviteter");
            return false;
        }
        var likeAktivitieter = p1.getAktiviteter().stream().allMatch(a1 -> p2.getAktiviteter().stream().anyMatch(a2 -> erLikAktivitet(a1, a2)));
        var sammenlign = p1.getTidsperiode().equals(p2.getTidsperiode()) &&
            Objects.equals(p1.isFlerbarnsdager(), p2.isFlerbarnsdager()) &&
            Objects.equals(p1.getResultatType(), p2.getResultatType()) &&
            Objects.equals(p1.getResultatÅrsak(), p2.getResultatÅrsak()) &&
            Objects.equals(p1.isGraderingInnvilget(), p2.isGraderingInnvilget()) &&
            Objects.equals(p1.isSamtidigUttak(), p2.isSamtidigUttak()) &&
            Objects.equals(p1.getUtsettelseType(), p2.getUtsettelseType()) &&
            Objects.equals(p1.getGraderingAvslagÅrsak(), p2.getGraderingAvslagÅrsak()) &&
            Objects.equals(p1.getSamtidigUttaksprosent(), p2.getSamtidigUttaksprosent()) &&
            Objects.equals(p1.getOppholdÅrsak(), p2.getOppholdÅrsak()) &&
            likeAktivitieter;
        if (!sammenlign)
            LOG.info("BEHRES avvik i periodedata, like aktiviteter {}", likeAktivitieter);
        return sammenlign;
    }

    private boolean erLikAktivitet(ForeldrepengerUttakPeriodeAktivitet a1, ForeldrepengerUttakPeriodeAktivitet a2) {
        return Objects.equals(a1.getUttakAktivitet(), a2.getUttakAktivitet()) &&
            Objects.equals(a1.getTrekkonto(), a2.getTrekkonto()) &&
            Objects.equals(a1.getTrekkdager(), a2.getTrekkdager()) &&
            (Objects.equals(a1.getArbeidsprosent(), a2.getArbeidsprosent()) || a1.getArbeidsprosent().compareTo(a2.getArbeidsprosent()) == 0) &&
            Objects.equals(a1.getUtbetalingsgrad(), a2.getUtbetalingsgrad());
    }

    private void nySammenlignLogg(List<ForeldrepengerUttakPeriode> uttaksPerioderTP, List<ForeldrepengerUttakPeriode> originaleUttaksPerioderTP, boolean gammelImpl) {
        LocalDateTimeline<WrapFUP> uttaksTL = new LocalDateTimeline<>(uttaksPerioderTP.stream()
            .map(p -> new WrapFUP(p.getTidsperiode(), p))
            .map(w -> new LocalDateSegment<>(w.getI(), w))
            .collect(Collectors.toList()))
            .compress(WrapFUP::erLikeNaboer, this::kombinerLikeNaboer);
        LocalDateTimeline<WrapFUP> originalTL = new LocalDateTimeline<>(originaleUttaksPerioderTP.stream()
            .map(p -> new WrapFUP(p.getTidsperiode(), p))
            .map(w -> new LocalDateSegment<>(w.getI(), w))
            .collect(Collectors.toList()))
            .compress(WrapFUP::erLikeNaboer, this::kombinerLikeNaboer);
        final boolean nyImpl;
        if (uttaksTL.getDatoIntervaller().size() != originalTL.getDatoIntervaller().size()) {
            nyImpl = false;
        } else {
            LocalDateTimeline<WrapFUP> kombinert = uttaksTL.combine(originalTL, this::fjernLikePerioder, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            nyImpl = !kombinert.filterValue(Objects::nonNull).getDatoIntervaller().isEmpty();
        }
        if (gammelImpl != nyImpl && vedtak != null)
            LOG.info("BEHRES TIMELINE behresId {} avvik gammel {} ny {} timeline {} original {}", vedtak.getBehandlingsresultat().getId(), gammelImpl, nyImpl, uttaksTL, originalTL);
    }

    private LocalDateSegment<WrapFUP> kombinerLikeNaboer(LocalDateInterval i, LocalDateSegment<WrapFUP> lhs, LocalDateSegment<WrapFUP> rhs) {
        if (lhs == null)
            return rhs;
        if (rhs == null)
            return lhs;
        return new LocalDateSegment<>(i, new WrapFUP(i, lhs.getValue(), rhs.getValue()));
    }

    private LocalDateSegment<WrapFUP> fjernLikePerioder(LocalDateInterval i, LocalDateSegment<WrapFUP> lhs, LocalDateSegment<WrapFUP> rhs) {
        if (lhs == null)
            return rhs;
        if (rhs == null)
            return lhs;
        // Kan ikke sammenligne splittede intervaller pga trekkdager - må være like for å eliminere
        if (!Objects.equals(lhs.getValue().getI(),rhs.getValue().getI()))
            return lhs;
        if (lhs.getValue().erLikePerioderTrekkdager(rhs.getValue()))
            return null;
        return lhs;
    }

    static class WrapFUP {
        private final LocalDateInterval i;
        private final ForeldrepengerUttakPeriode p;
        private final Map<ForeldrepengerUttakAktivitet, Trekkdager> t;

        WrapFUP(LocalDateInterval i, ForeldrepengerUttakPeriode p) {
            this.i = i;
            this.p = p;
            this.t = p.getAktiviteter().stream()
                .collect(Collectors.groupingBy(ForeldrepengerUttakPeriodeAktivitet::getUttakAktivitet,
                    Collectors.reducing(Trekkdager.ZERO, ForeldrepengerUttakPeriodeAktivitet::getTrekkdager, Trekkdager::add)));
        }

        WrapFUP(LocalDateInterval i, WrapFUP p1, WrapFUP p2) {
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WrapFUP wrapFUP = (WrapFUP) o;
            return erLikeNaboPerioder(this.getP(), wrapFUP.getP());
        }

        public boolean erLikePerioderTrekkdager(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WrapFUP wrapFUP = (WrapFUP) o;
            return erLikeNaboPerioder(this.getP(), wrapFUP.getP()) &&
                Objects.equals(t, wrapFUP.t);
        }

    }

    private static boolean erLikeNaboPerioder(ForeldrepengerUttakPeriode p1, ForeldrepengerUttakPeriode p2) {
        if (p1.getAktiviteter().size() != p2.getAktiviteter().size()) {
            LOG.info("BEHRES avvik antall aktiviteter");
            return false;
        }
        var likeAktivitieter = p1.getAktiviteter().stream().allMatch(a1 -> p2.getAktiviteter().stream().anyMatch(a2 -> erLikNaboAktivitet(a1, a2)));
        return Objects.equals(p1.isFlerbarnsdager(), p2.isFlerbarnsdager()) &&
            Objects.equals(p1.getResultatType(), p2.getResultatType()) &&
            Objects.equals(p1.getResultatÅrsak(), p2.getResultatÅrsak()) &&
            Objects.equals(p1.isSamtidigUttak(), p2.isSamtidigUttak()) &&
            Objects.equals(p1.isGraderingInnvilget(), p2.isGraderingInnvilget()) &&
            Objects.equals(p1.getUtsettelseType(), p2.getUtsettelseType()) &&
            Objects.equals(p1.getGraderingAvslagÅrsak(), p2.getGraderingAvslagÅrsak()) &&
            Objects.equals(p1.getSamtidigUttaksprosent(), p2.getSamtidigUttaksprosent()) &&
            Objects.equals(p1.getOppholdÅrsak(), p2.getOppholdÅrsak()) &&
            likeAktivitieter;
    }

    private static boolean erLikNaboAktivitet(ForeldrepengerUttakPeriodeAktivitet a1, ForeldrepengerUttakPeriodeAktivitet a2) {
        if (Objects.equals(a1.getTrekkdager(), Trekkdager.ZERO) && Objects.equals(a2.getTrekkdager(), Trekkdager.ZERO))
            return Objects.equals(a1.getUttakAktivitet(), a2.getUttakAktivitet());
        if (Objects.equals(a1.getTrekkdager(), Trekkdager.ZERO) || Objects.equals(a2.getTrekkdager(), Trekkdager.ZERO))
            return false;
        return a1.likBortsettFraTrekkdager(a2);
    }

}
