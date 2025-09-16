package no.nav.foreldrepenger.domene.modell;

import java.io.Serializable;
import java.util.Objects;

import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaVurderingKilde;

public class FaktaVurdering implements Serializable {


    private Boolean vurdering;
    private FaktaVurderingKilde kilde;


    public FaktaVurdering() {
        // hibernate
    }

    public FaktaVurdering(Boolean vurdering, FaktaVurderingKilde kilde) {
        this.vurdering = vurdering;
        this.kilde = kilde;
    }


    public Boolean getVurdering() {
        return vurdering;
    }

    public FaktaVurderingKilde getKilde() {
        return kilde;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (FaktaVurdering) o;
        return vurdering.equals(that.vurdering) && kilde == that.kilde;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vurdering, kilde);
    }

    @Override
    public String toString() {
        return "FaktaVurdering{" +
            "vurdering=" + vurdering +
            ", kilde=" + kilde +
            '}';
    }
}
