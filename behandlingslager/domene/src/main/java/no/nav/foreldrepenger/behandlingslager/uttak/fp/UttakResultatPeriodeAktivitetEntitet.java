package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity
@Table(name = "UTTAK_RESULTAT_PERIODE_AKT")
public class UttakResultatPeriodeAktivitetEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTTAK_RESULTAT_PERIODE_AKT")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ManyToOne(optional = false)
    @JoinColumn(name = "uttak_resultat_periode_id", nullable = false, updatable = false)
    private UttakResultatPeriodeEntitet periode;

    @ManyToOne(optional = false)
    @JoinColumn(name = "uttak_aktivitet_id", nullable = false, updatable = false)
    private UttakAktivitetEntitet uttakAktivitet;

    @Convert(converter = StønadskontoType.KodeverdiConverter.class)
    @Column(name="trekkonto", nullable = false)
    private StønadskontoType trekkonto = StønadskontoType.UDEFINERT;

    @Embedded
    private Trekkdager trekkdagerDesimal;

    @Column(name = "arbeidstidsprosent", nullable = false)
    private BigDecimal arbeidsprosent;

    @Embedded
    private Utbetalingsgrad utbetalingsgrad;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "gradering", updatable = false, nullable = false)
    private boolean gradering;

    @Override
    public String toString() {
        return "UttakResultatPeriodeAktivitetEntitet{" +
            "periode=" + periode.getId() +
            ", trekkonto=" + trekkonto +
            ", trekkdagerDesimal='" + trekkdagerDesimal + '\'' +
            ", arbeidsprosent=" + arbeidsprosent +
            ", utbetalingsgrad=" + utbetalingsgrad +
            ", uttakAktivitet=" + uttakAktivitet +
            ", gradering=" + gradering +
            '}';
    }

    public Long getId() {
        return id;
    }

    public Trekkdager getTrekkdager() {
        return new Trekkdager(trekkdagerDesimal.decimalValue());
    }

    public StønadskontoType getTrekkonto() {
        return trekkonto;
    }

    public BigDecimal getArbeidsprosent() {
        return arbeidsprosent;
    }

    public Stillingsprosent getArbeidsprosentSomStillingsprosent() {
        return new Stillingsprosent(arbeidsprosent);
    }

    public Utbetalingsgrad getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public UttakResultatPeriodeEntitet getPeriode() {
        return periode;
    }

    public UttakAktivitetEntitet getUttakAktivitet() {
        return uttakAktivitet;
    }

    public static Builder builder(UttakResultatPeriodeEntitet periode, UttakAktivitetEntitet uttakAktivitet) {
        return new Builder(periode, uttakAktivitet);
    }

    public void setPeriode(UttakResultatPeriodeEntitet periode) {
        this.periode = periode;
    }

    public void setUttakAktivitet(UttakAktivitetEntitet uttakAktivitet) {
        this.uttakAktivitet = uttakAktivitet;
    }

    public LocalDate getFom() {
        return this.periode.getFom();
    }

    public LocalDate getTom() {
        return this.periode.getTom();
    }

    public boolean isGraderingInnvilget() {
        return periode.isGraderingInnvilget() && isSøktGradering();
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return uttakAktivitet.getArbeidsforholdRef();
    }

    public Arbeidsgiver getArbeidsgiver() {
        return uttakAktivitet.getArbeidsgiver().isPresent() ? uttakAktivitet.getArbeidsgiver().get() : null;
    }

    public UttakArbeidType getUttakArbeidType() {
        return uttakAktivitet.getUttakArbeidType();
    }

    public boolean isSøktGradering() {
        return gradering;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (UttakResultatPeriodeAktivitetEntitet) o;
        return Objects.equals(periode, that.periode) &&
            Objects.equals(uttakAktivitet, that.uttakAktivitet);
    }

    @Override
    public int hashCode() {

        return Objects.hash(periode, uttakAktivitet);
    }

    public static class Builder {
        UttakResultatPeriodeAktivitetEntitet kladd;

        public Builder(UttakResultatPeriodeEntitet periode, UttakAktivitetEntitet uttakAktivitet) {
            kladd = new UttakResultatPeriodeAktivitetEntitet();
            kladd.periode = periode;
            kladd.uttakAktivitet = uttakAktivitet;
            kladd.trekkdagerDesimal = Trekkdager.ZERO;
            periode.leggTilAktivitet(kladd);
        }

        public UttakResultatPeriodeAktivitetEntitet.Builder medTrekkdager(Trekkdager trekkdager) {
            kladd.trekkdagerDesimal = trekkdager;
            return this;
        }

        public UttakResultatPeriodeAktivitetEntitet.Builder medTrekkonto(StønadskontoType trekkonto) {
            kladd.trekkonto = trekkonto;
            return this;
        }

        public UttakResultatPeriodeAktivitetEntitet.Builder medArbeidsprosent(BigDecimal arbeidsprosent) {
            kladd.arbeidsprosent = arbeidsprosent;
            return this;
        }

        public UttakResultatPeriodeAktivitetEntitet.Builder medUtbetalingsgrad(Utbetalingsgrad utbetalingsgrad) {
            kladd.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        public UttakResultatPeriodeAktivitetEntitet.Builder medErSøktGradering(boolean gradering) {
            kladd.gradering = gradering;
            return this;
        }

        public UttakResultatPeriodeAktivitetEntitet build() {
            Objects.requireNonNull(kladd.periode, "periode");
            Objects.requireNonNull(kladd.arbeidsprosent, "arbeidsprosent");
            Objects.requireNonNull(kladd.uttakAktivitet, "uttakAktivitet");
            return kladd;
        }
    }
}
