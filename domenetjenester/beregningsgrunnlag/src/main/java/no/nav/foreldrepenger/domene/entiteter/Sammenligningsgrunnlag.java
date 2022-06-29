package no.nav.foreldrepenger.domene.entiteter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@Entity(name = "Sammenligningsgrunnlag")
@Table(name = "SAMMENLIGNINGSGRUNNLAG")
public class Sammenligningsgrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SAMMENLIGNINGSGRUNNLAG")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "sammenligningsperiode_fom")),
        @AttributeOverride(name = "tomDato", column = @Column(name = "sammenligningsperiode_tom"))
    })
    private DatoIntervallEntitet sammenligningsperiode;

    @Column(name = "rapportert_pr_aar", nullable = false)
    private BigDecimal rapportertPrÅr;

    @Column(name = "avvik_promille", nullable = false)
    private BigDecimal avvikPromille = BigDecimal.ZERO;

    @JsonBackReference
    @OneToOne(optional = false)
    @JoinColumn(name = "beregningsgrunnlag_id", nullable = false, updatable = false, unique = true)
    private BeregningsgrunnlagEntitet beregningsgrunnlag;

    public Sammenligningsgrunnlag(Sammenligningsgrunnlag sammenligningsgrunnlag) {
        this.avvikPromille = sammenligningsgrunnlag.getAvvikPromille();
        this.rapportertPrÅr = sammenligningsgrunnlag.getRapportertPrÅr();
        this.sammenligningsperiode = sammenligningsgrunnlag.getSammenligningsperiode();
    }

    public Sammenligningsgrunnlag() {
        // NOSONAR
    }

    public Long getId() {
        return id;
    }

    public LocalDate getSammenligningsperiodeFom() {
        return sammenligningsperiode.getFomDato();
    }

    public LocalDate getSammenligningsperiodeTom() {
        return sammenligningsperiode.getTomDato();
    }

    public BigDecimal getRapportertPrÅr() {
        return rapportertPrÅr;
    }

    public BigDecimal getAvvikPromille() {
        return avvikPromille;
    }

    public BeregningsgrunnlagEntitet getBeregningsgrunnlag() {
        return beregningsgrunnlag;
    }

    public DatoIntervallEntitet getSammenligningsperiode() {
        return sammenligningsperiode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Sammenligningsgrunnlag)) {
            return false;
        }
        var other = (Sammenligningsgrunnlag) obj;
        return Objects.equals(this.getBeregningsgrunnlag(), other.getBeregningsgrunnlag())
                && Objects.equals(this.getSammenligningsperiodeFom(), other.getSammenligningsperiodeFom())
                && Objects.equals(this.getSammenligningsperiodeTom(), other.getSammenligningsperiodeTom())
                && Objects.equals(this.getRapportertPrÅr(), other.getRapportertPrÅr());
    }

    @Override
    public int hashCode() {
        return Objects.hash(beregningsgrunnlag, sammenligningsperiode, rapportertPrÅr, avvikPromille);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
                "id=" + id + ", " //$NON-NLS-2$
                + "beregningsgrunnlag=" + beregningsgrunnlag + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "sammenligningsperiodeFom=" + sammenligningsperiode.getFomDato() + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "sammenligningsperiodeTom=" + sammenligningsperiode.getTomDato() + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "rapportertPrÅr=" + rapportertPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "avvikPromille=" + avvikPromille + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + ">"; //$NON-NLS-1$
    }

    void setBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        this.beregningsgrunnlag = beregningsgrunnlag;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Sammenligningsgrunnlag sammenligningsgrunnlagMal;

        public Builder() {
            sammenligningsgrunnlagMal = new Sammenligningsgrunnlag();
        }

        public Builder medSammenligningsperiode(LocalDate fom, LocalDate tom) {
            sammenligningsgrunnlagMal.sammenligningsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
            return this;
        }

        public Builder medRapportertPrÅr(BigDecimal rapportertPrÅr) {
            sammenligningsgrunnlagMal.rapportertPrÅr = rapportertPrÅr;
            return this;
        }

        public Builder medAvvikPromille(BigDecimal avvikPromille) {
            if(avvikPromille != null) {
                sammenligningsgrunnlagMal.avvikPromille = avvikPromille;
            }
            return this;
        }

        public Sammenligningsgrunnlag build(BeregningsgrunnlagEntitet beregningsgrunnlag) {
            sammenligningsgrunnlagMal.beregningsgrunnlag = beregningsgrunnlag;
            verifyStateForBuild();
            beregningsgrunnlag.setSammenligningsgrunnlag(sammenligningsgrunnlagMal);
            return sammenligningsgrunnlagMal;
        }

        public Sammenligningsgrunnlag build() {
            verifyStateForBuild();
            return sammenligningsgrunnlagMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(sammenligningsgrunnlagMal.sammenligningsperiode, "sammenligningsperiodePeriode");
            Objects.requireNonNull(sammenligningsgrunnlagMal.sammenligningsperiode.getFomDato(), "sammenligningsperiodeFom");
            Objects.requireNonNull(sammenligningsgrunnlagMal.sammenligningsperiode.getTomDato(), "sammenligningsperiodeTom");
            Objects.requireNonNull(sammenligningsgrunnlagMal.rapportertPrÅr, "rapportertPrÅr");
            Objects.requireNonNull(sammenligningsgrunnlagMal.avvikPromille, "avvikPromille");
        }
    }

}
