package no.nav.foreldrepenger.inngangsvilkaar;

import java.util.List;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;

public record RegelResultat(VilkårResultat vilkårResultat,
                         List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner,
                         Map<VilkårType, Object> ekstraResultater) {

}
