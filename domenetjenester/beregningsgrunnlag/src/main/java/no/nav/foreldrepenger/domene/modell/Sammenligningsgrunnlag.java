package no.nav.foreldrepenger.domene.modell;

import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;


public class Sammenligningsgrunnlag {

    private DatoIntervallEntitet sammenligningsperiode;
    private BigDecimal rapportertPrÅr;
    private Long avvikPromille = 0L;

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


    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Sammenligningsgrunnlag)) {
            return false;
        }
        var other = (Sammenligningsgrunnlag) obj;
        return Objects.equals(this.getSammenligningsperiodeFom(), other.getSammenligningsperiodeFom())
                && Objects.equals(this.getSammenligningsperiodeTom(), other.getSammenligningsperiodeTom())
                && Objects.equals(this.getRapportertPrÅr(), other.getRapportertPrÅr());
    }

    @Override
    public int hashCode() {
        return Objects.hash(sammenligningsperiode, rapportertPrÅr, avvikPromille);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
                + "sammenligningsperiodeFom=" + sammenligningsperiode.getFomDato() + ", "
                + "sammenligningsperiodeTom=" + sammenligningsperiode.getTomDato() + ", "
                + "rapportertPrÅr=" + rapportertPrÅr + ", "
                + "avvikPromille=" + avvikPromille + ", "
                + ">";
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
