package no.nav.foreldrepenger.behandling.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;

public record AksjonspunktUtlederResultat(AksjonspunktDefinisjon aksjonspunktDefinisjon, Venteårsak venteårsak, LocalDateTime frist) {

    public static AksjonspunktUtlederResultat opprettForAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return new AksjonspunktUtlederResultat(aksjonspunktDefinisjon, null, null);
    }

    public static AksjonspunktUtlederResultat opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon aksjonspunktDefinisjon, Venteårsak venteårsak, LocalDateTime frist) {
        return new AksjonspunktUtlederResultat(aksjonspunktDefinisjon, venteårsak, frist);
    }

    public static List<AksjonspunktUtlederResultat> opprettListeForAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return List.of(new AksjonspunktUtlederResultat(aksjonspunktDefinisjon, null, null));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AksjonspunktUtlederResultat that &&  aksjonspunktDefinisjon == that.aksjonspunktDefinisjon;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(aksjonspunktDefinisjon);
    }
}
