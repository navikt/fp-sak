package no.nav.foreldrepenger.inngangsvilkaar;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;

import java.util.List;
import java.util.Map;

public record RegelResultat(VilkårResultat vilkårResultat,
                         List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner,
                         Map<VilkårType, Object> ekstraResultater) {

}
