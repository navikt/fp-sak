package no.nav.foreldrepenger.domene.modell;

import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BesteberegningMånedsgrunnlag {

    private DatoIntervallEntitet periode;
    private List<BesteberegningInntekt> inntekter = new ArrayList<>();

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public List<BesteberegningInntekt> getInntekter() {
        return Collections.unmodifiableList(inntekter);
    }

    private void leggTilInntekt(BesteberegningInntekt besteberegningInntekt) {
        this.inntekter.add(besteberegningInntekt);
    }

    public static Builder ny() {
        return new Builder();
    }

    public static class Builder {
        private final BesteberegningMånedsgrunnlag kladd;

        public Builder() {
            kladd = new BesteberegningMånedsgrunnlag();
        }

        public Builder leggTilInntekt(BesteberegningInntekt besteberegningInntekt) {
            kladd.leggTilInntekt(besteberegningInntekt);
            return this;
        }

        public Builder medPeriode(LocalDate fom, LocalDate tom) {
            kladd.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
            return this;
        }

        public BesteberegningMånedsgrunnlag build() {
            Objects.requireNonNull(kladd.periode);
            return kladd;
        }

    }

}
