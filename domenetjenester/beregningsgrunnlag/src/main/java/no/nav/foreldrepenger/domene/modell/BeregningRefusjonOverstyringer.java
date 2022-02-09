package no.nav.foreldrepenger.domene.modell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BeregningRefusjonOverstyringer {

    private List<BeregningRefusjonOverstyring> overstyringer = new ArrayList<>();

    protected BeregningRefusjonOverstyringer() {
        // Hibernate
    }

    public List<BeregningRefusjonOverstyring> getRefusjonOverstyringer() {
        return Collections.unmodifiableList(overstyringer);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BeregningRefusjonOverstyringer kladd;

        private Builder() {
            kladd = new BeregningRefusjonOverstyringer();
        }

        public Builder leggTilOverstyring(BeregningRefusjonOverstyring beregningRefusjonOverstyring) {
            BeregningRefusjonOverstyring entitet = beregningRefusjonOverstyring;
            kladd.overstyringer.add(entitet);
            return this;
        }

        public BeregningRefusjonOverstyringer build() {
            return kladd;
        }
    }
}
