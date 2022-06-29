package no.nav.foreldrepenger.domene.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;


public class Sammenligningsgrunnlag {

    private DatoIntervallEntitet sammenligningsperiode;
    private BigDecimal rapportertPrÅr;
    private Long avvikPromille = 0L;
    private Beregningsgrunnlag beregningsgrunnlag;

    public LocalDate getSammenligningsperiodeFom() {
        return sammenligningsperiode.getFomDato();
    }

    public LocalDate getSammenligningsperiodeTom() {
        return sammenligningsperiode.getTomDato();
    }

    public BigDecimal getRapportertPrÅr() {
        return rapportertPrÅr;
    }

    public Long getAvvikPromille() {
        return avvikPromille;
    }

    public Beregningsgrunnlag getBeregningsgrunnlag() {
        return beregningsgrunnlag;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Sammenligningsgrunnlag)) {
            return false;
        }
        Sammenligningsgrunnlag other = (Sammenligningsgrunnlag) obj;
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
        return getClass().getSimpleName() + "<"  //$NON-NLS-1$
                + "beregningsgrunnlag=" + beregningsgrunnlag + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "sammenligningsperiodeFom=" + sammenligningsperiode.getFomDato() + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "sammenligningsperiodeTom=" + sammenligningsperiode.getTomDato() + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "rapportertPrÅr=" + rapportertPrÅr + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + "avvikPromille=" + avvikPromille + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + ">"; //$NON-NLS-1$
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

        public Builder medAvvikPromille(Long avvikPromille) {
            if(avvikPromille != null) {
                sammenligningsgrunnlagMal.avvikPromille = avvikPromille;
            }
            return this;
        }

        public Sammenligningsgrunnlag build(Beregningsgrunnlag beregningsgrunnlag) {
            sammenligningsgrunnlagMal.beregningsgrunnlag = beregningsgrunnlag;
            verifyStateForBuild();
            Beregningsgrunnlag.builder(beregningsgrunnlag).medSammenligningsgrunnlag(sammenligningsgrunnlagMal);
            return sammenligningsgrunnlagMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(sammenligningsgrunnlagMal.beregningsgrunnlag, "beregningsgrunnlag");
            Objects.requireNonNull(sammenligningsgrunnlagMal.sammenligningsperiode, "sammenligningsperiodePeriode");
            Objects.requireNonNull(sammenligningsgrunnlagMal.sammenligningsperiode.getFomDato(), "sammenligningsperiodeFom");
            Objects.requireNonNull(sammenligningsgrunnlagMal.sammenligningsperiode.getTomDato(), "sammenligningsperiodeTom");
            Objects.requireNonNull(sammenligningsgrunnlagMal.rapportertPrÅr, "rapportertPrÅr");
            Objects.requireNonNull(sammenligningsgrunnlagMal.avvikPromille, "avvikPromille");
        }
    }

}
