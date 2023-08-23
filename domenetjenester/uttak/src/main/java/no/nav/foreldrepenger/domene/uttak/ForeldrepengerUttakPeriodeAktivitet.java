package no.nav.foreldrepenger.domene.uttak;

import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public class ForeldrepengerUttakPeriodeAktivitet {

    private ForeldrepengerUttakAktivitet aktivitet;
    private StønadskontoType trekkonto = StønadskontoType.UDEFINERT;
    private Trekkdager trekkdager = Trekkdager.ZERO;
    private BigDecimal arbeidsprosent;
    private Utbetalingsgrad utbetalingsgrad;
    private boolean søktGraderingForAktivitetIPeriode;

    private ForeldrepengerUttakPeriodeAktivitet() {
    }

    public StønadskontoType getTrekkonto() {
        return trekkonto;
    }

    public Trekkdager getTrekkdager() {
        return trekkdager;
    }

    public BigDecimal getArbeidsprosent() {
        return arbeidsprosent;
    }

    public Utbetalingsgrad getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public boolean isSøktGraderingForAktivitetIPeriode() {
        return søktGraderingForAktivitetIPeriode;
    }

    public ForeldrepengerUttakAktivitet getUttakAktivitet() {
        return aktivitet;
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return getUttakAktivitet().getArbeidsgiver();
    }

    public UttakArbeidType getUttakArbeidType() {
        return getUttakAktivitet().getUttakArbeidType();
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return getUttakAktivitet().getArbeidsforholdRef();
    }

    @Override
    public String toString() {
        return "ForeldrepengerUttakPeriodeAktivitet{" +
            "aktivitet=" + aktivitet +
            ", trekkonto=" + trekkonto +
            ", trekkdager=" + trekkdager +
            ", arbeidsprosent=" + arbeidsprosent +
            ", utbetalingsgrad=" + utbetalingsgrad +
            ", søktGraderingForAktivitetIPeriode=" + søktGraderingForAktivitetIPeriode +
            '}';
    }

    public static class Builder {
        private ForeldrepengerUttakPeriodeAktivitet kladd = new ForeldrepengerUttakPeriodeAktivitet();

        public Builder medTrekkonto(StønadskontoType stønadskontoType) {
            kladd.trekkonto = stønadskontoType;
            return this;
        }

        public Builder medTrekkdager(Trekkdager trekkdager) {
            kladd.trekkdager = trekkdager;
            return this;
        }

        public Builder medArbeidsprosent(BigDecimal arbeidsprosent) {
            kladd.arbeidsprosent = arbeidsprosent;
            return this;
        }

        public Builder medAktivitet(ForeldrepengerUttakAktivitet aktivitet) {
            kladd.aktivitet = aktivitet;
            return this;
        }

        public Builder medUtbetalingsgrad(Utbetalingsgrad utbetalingsgrad) {
            kladd.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        public Builder medSøktGraderingForAktivitetIPeriode(boolean søktGradering) {
            kladd.søktGraderingForAktivitetIPeriode = søktGradering;
            return this;
        }

        public ForeldrepengerUttakPeriodeAktivitet build() {
            Objects.requireNonNull(kladd.arbeidsprosent, "arbeidsprosent");
            Objects.requireNonNull(kladd.aktivitet, "aktivitet");
            return kladd;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (ForeldrepengerUttakPeriodeAktivitet) o;
        return trekkdager.equals(that.trekkdager) && likBortsettFraTrekkdager(that);
    }

    public boolean likBortsettFraTrekkdager(ForeldrepengerUttakPeriodeAktivitet that) {
        return Objects.equals(trekkonto, that.trekkonto) &&
            (Objects.equals(arbeidsprosent, that.arbeidsprosent) || arbeidsprosent.compareTo(that.arbeidsprosent) == 0) &&
            Objects.equals(utbetalingsgrad, that.utbetalingsgrad) &&
            Objects.equals(aktivitet, that.aktivitet);
    }

    public boolean likEllerSammeAktivitetZeroTrekkdager(ForeldrepengerUttakPeriodeAktivitet that) {
        if (Objects.equals(trekkdager, Trekkdager.ZERO) && Objects.equals(that.getTrekkdager(), Trekkdager.ZERO))
            return Objects.equals(aktivitet, that.getUttakAktivitet());
        if (Objects.equals(trekkdager, Trekkdager.ZERO) || Objects.equals(that.getTrekkdager(), Trekkdager.ZERO))
            return false;
        return likBortsettFraTrekkdager(that);
    }


    @Override
    public int hashCode() {
        return Objects.hash(trekkonto, trekkdager, arbeidsprosent, utbetalingsgrad, aktivitet);
    }
}
