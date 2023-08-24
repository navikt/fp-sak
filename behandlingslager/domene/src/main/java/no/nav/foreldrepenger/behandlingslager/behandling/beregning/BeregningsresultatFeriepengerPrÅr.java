package no.nav.foreldrepenger.behandlingslager.behandling.beregning;


import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.domene.typer.Beløp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity(name = "BeregningsresultatFeriepengerPrÅr")
@Table(name = "BR_FERIEPENGER_PR_AAR")
public class BeregningsresultatFeriepengerPrÅr extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BR_FERIEPENGER_PR_AAR")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ManyToOne(optional = false)
    @JoinColumn(name = "br_feriepenger_id", nullable = false, updatable = false)
    private BeregningsresultatFeriepenger beregningsresultatFeriepenger;

    @ManyToOne(optional = false)
    @JoinColumn(name = "beregningsresultat_andel_id", nullable = false, updatable = false)
    private BeregningsresultatAndel beregningsresultatAndel;

    @Column(name = "opptjeningsaar", nullable = false)
    private LocalDate opptjeningsår;

    @Embedded
    @AttributeOverride(name = "verdi", column = @Column(name = "aarsbeloep", nullable = false))
    @ChangeTracked
    private Beløp årsbeløp;

    public Long getId() {
        return id;
    }

    public BeregningsresultatFeriepenger getBeregningsresultatFeriepenger() {
        return beregningsresultatFeriepenger;
    }

    public BeregningsresultatAndel getBeregningsresultatAndel() {
        return beregningsresultatAndel;
    }

    public LocalDate getOpptjeningsår() {
        return opptjeningsår;
    }

    public int getOpptjeningsåret() {
        return opptjeningsår.getYear();
    }

    public Beløp getÅrsbeløp() {
        return årsbeløp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BeregningsresultatFeriepengerPrÅr other)) {
            return false;
        }
        return Objects.equals(this.getOpptjeningsår(), other.getOpptjeningsår())
            && Objects.equals(this.getÅrsbeløp(), other.getÅrsbeløp());
    }

    @Override
    public int hashCode() {
        return Objects.hash(opptjeningsår, årsbeløp);
    }

    @Override
    public String toString() {
        return "BRFerPrÅr{" +
            "brFerie=" + beregningsresultatFeriepenger +
            ", opptjeningsår=" + opptjeningsår +
            ", årsbeløp=" + årsbeløp +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BeregningsresultatFeriepengerPrÅr beregningsresultatFeriepengerPrÅrMal;

        public Builder() {
            beregningsresultatFeriepengerPrÅrMal = new BeregningsresultatFeriepengerPrÅr();
        }

        public Builder medOpptjeningsår(int opptjeningsår) {
            beregningsresultatFeriepengerPrÅrMal.opptjeningsår = LocalDate.of(opptjeningsår, 12, 31);
            return this;
        }

        public Builder medOpptjeningsår(LocalDate opptjeningsår) {
            beregningsresultatFeriepengerPrÅrMal.opptjeningsår = opptjeningsår;
            return this;
        }

        public Builder medÅrsbeløp(int årsbeløp) {
            beregningsresultatFeriepengerPrÅrMal.årsbeløp = new Beløp(BigDecimal.valueOf(årsbeløp));
            return this;
        }

        public Builder medÅrsbeløp(Long årsbeløp) {
            beregningsresultatFeriepengerPrÅrMal.årsbeløp = new Beløp(BigDecimal.valueOf(årsbeløp));
            return this;
        }

        public BeregningsresultatFeriepengerPrÅr build(BeregningsresultatFeriepenger beregningsresultatFeriepenger, BeregningsresultatAndel beregningsresultatAndel) {
            beregningsresultatFeriepengerPrÅrMal.beregningsresultatFeriepenger = beregningsresultatFeriepenger;
            BeregningsresultatFeriepenger.builder(beregningsresultatFeriepenger).leggTilBeregningsresultatFeriepengerPrÅr(beregningsresultatFeriepengerPrÅrMal);
            beregningsresultatFeriepengerPrÅrMal.beregningsresultatAndel = beregningsresultatAndel;
            BeregningsresultatAndel.builder(beregningsresultatAndel).leggTilBeregningsresultatFeriepengerPrÅr(beregningsresultatFeriepengerPrÅrMal);
            verifyStateForBuild();
            return beregningsresultatFeriepengerPrÅrMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(beregningsresultatFeriepengerPrÅrMal.beregningsresultatFeriepenger, "beregningsresultatFeriepenger");
            Objects.requireNonNull(beregningsresultatFeriepengerPrÅrMal.beregningsresultatAndel, "beregningsresultatAndel");
            Objects.requireNonNull(beregningsresultatFeriepengerPrÅrMal.opptjeningsår, "opptjeningsår");
            Objects.requireNonNull(beregningsresultatFeriepengerPrÅrMal.årsbeløp, "årsbeløp");
        }
    }
}
