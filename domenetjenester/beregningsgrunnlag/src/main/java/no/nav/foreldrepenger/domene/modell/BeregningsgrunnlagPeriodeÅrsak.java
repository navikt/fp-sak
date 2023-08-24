package no.nav.foreldrepenger.domene.modell;


import no.nav.foreldrepenger.domene.modell.kodeverk.PeriodeÅrsak;

import java.util.Objects;


public class BeregningsgrunnlagPeriodeÅrsak {

    private BeregningsgrunnlagPeriode beregningsgrunnlagPeriode;
    private PeriodeÅrsak periodeÅrsak = PeriodeÅrsak.UDEFINERT;

    public BeregningsgrunnlagPeriode getBeregningsgrunnlagPeriode() {
        return beregningsgrunnlagPeriode;
    }

    public PeriodeÅrsak getPeriodeÅrsak() {
        return periodeÅrsak;
    }


    public void setBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        this.beregningsgrunnlagPeriode = beregningsgrunnlagPeriode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(beregningsgrunnlagPeriode, periodeÅrsak);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof BeregningsgrunnlagPeriodeÅrsak)) {
            return false;
        }
        var other = (BeregningsgrunnlagPeriodeÅrsak) obj;
        return Objects.equals(this.getBeregningsgrunnlagPeriode(), other.getBeregningsgrunnlagPeriode())
                && Objects.equals(this.getPeriodeÅrsak(), other.getPeriodeÅrsak());
    }

    public static class Builder {
        private BeregningsgrunnlagPeriodeÅrsak beregningsgrunnlagPeriodeÅrsakMal;

        public Builder() {
            beregningsgrunnlagPeriodeÅrsakMal = new BeregningsgrunnlagPeriodeÅrsak();
        }

        public Builder medPeriodeÅrsak(PeriodeÅrsak periodeÅrsak) {
            beregningsgrunnlagPeriodeÅrsakMal.periodeÅrsak = periodeÅrsak;
            return this;
        }

        public BeregningsgrunnlagPeriodeÅrsak build(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
            beregningsgrunnlagPeriodeÅrsakMal.beregningsgrunnlagPeriode = beregningsgrunnlagPeriode;
            beregningsgrunnlagPeriode.addBeregningsgrunnlagPeriodeÅrsak(beregningsgrunnlagPeriodeÅrsakMal);
            return beregningsgrunnlagPeriodeÅrsakMal;
        }
    }
}
