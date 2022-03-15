package no.nav.foreldrepenger.domene.modell;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;

public class BeregningsgrunnlagAktivitetStatus {

    private Beregningsgrunnlag beregningsgrunnlag;
    private AktivitetStatus aktivitetStatus;
    private Hjemmel hjemmel;

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BeregningsgrunnlagAktivitetStatus beregningsgrunnlagAktivitetStatusMal) {
        return new Builder(beregningsgrunnlagAktivitetStatusMal);
    }

    public Beregningsgrunnlag getBeregningsgrunnlag() {
        return beregningsgrunnlag;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public Hjemmel getHjemmel() {
        return hjemmel;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof BeregningsgrunnlagAktivitetStatus)) {
            return false;
        }
        BeregningsgrunnlagAktivitetStatus other = (BeregningsgrunnlagAktivitetStatus) obj;
        return Objects.equals(this.getAktivitetStatus(), other.getAktivitetStatus())
            && Objects.equals(this.getBeregningsgrunnlag(), other.getBeregningsgrunnlag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(beregningsgrunnlag, aktivitetStatus);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" //$NON-NLS-1$
            + "beregningsgrunnlag=" + beregningsgrunnlag + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "aktivitetStatus=" + aktivitetStatus + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "hjemmel=" + hjemmel + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + ">"; //$NON-NLS-1$
    }

    public static class Builder {
        private BeregningsgrunnlagAktivitetStatus beregningsgrunnlagAktivitetStatusMal;

        public Builder(BeregningsgrunnlagAktivitetStatus beregningsgrunnlagAktivitetStatusMal) {
            this.beregningsgrunnlagAktivitetStatusMal = beregningsgrunnlagAktivitetStatusMal;
        }

        public Builder() {
            beregningsgrunnlagAktivitetStatusMal = new BeregningsgrunnlagAktivitetStatus();
            beregningsgrunnlagAktivitetStatusMal.hjemmel = Hjemmel.UDEFINERT;
        }

        public Builder medAktivitetStatus(AktivitetStatus aktivitetStatus) {
            beregningsgrunnlagAktivitetStatusMal.aktivitetStatus = aktivitetStatus;
            return this;
        }

        public Builder medHjemmel(Hjemmel hjemmel) {
            beregningsgrunnlagAktivitetStatusMal.hjemmel = hjemmel;
            return this;
        }

        public BeregningsgrunnlagAktivitetStatus build(Beregningsgrunnlag beregningsgrunnlag) {
            beregningsgrunnlagAktivitetStatusMal.beregningsgrunnlag = beregningsgrunnlag;
            verifyStateForBuild();
            beregningsgrunnlag.leggTilBeregningsgrunnlagAktivitetStatus(beregningsgrunnlagAktivitetStatusMal);
            return beregningsgrunnlagAktivitetStatusMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(beregningsgrunnlagAktivitetStatusMal.beregningsgrunnlag, "beregningsgrunnlag");
            Objects.requireNonNull(beregningsgrunnlagAktivitetStatusMal.aktivitetStatus, "aktivitetStatus");
            Objects.requireNonNull(beregningsgrunnlagAktivitetStatusMal.getHjemmel(), "hjemmel");
        }
    }
}
