package no.nav.foreldrepenger.domene.modell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BeregningAktivitetOverstyringer {

    private List<BeregningAktivitetOverstyring> overstyringer = new ArrayList<>();

    public List<BeregningAktivitetOverstyring> getOverstyringer() {
        return Collections.unmodifiableList(overstyringer);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BeregningAktivitetOverstyringer kladd;

        private Builder() {
            kladd = new BeregningAktivitetOverstyringer();
        }

        public Builder leggTilOverstyring(BeregningAktivitetOverstyring beregningAktivitetOverstyring) {
            kladd.overstyringer.add(beregningAktivitetOverstyring);
            return this;
        }

        public BeregningAktivitetOverstyringer build() {
            return kladd;
        }
    }
}
