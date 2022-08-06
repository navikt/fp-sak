package no.nav.foreldrepenger.inngangsvilkaar;

import java.util.List;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

public record VilkårData(VilkårType vilkårType, VilkårUtfallType utfallType, Map<String, Object> merknadParametere,
            List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner, VilkårUtfallMerknad vilkårUtfallMerknad,
            String regelEvaluering, String regelInput, Object ekstraVilkårresultat) {

    /** Ctor som tar minimum av parametere, og ingen regel evaluering og input data.  Vil heller aldri være overstyrt. */
    public VilkårData(VilkårType vilkårType, VilkårUtfallType utfallType, List<AksjonspunktDefinisjon> apDefinisjoner) {
        this(vilkårType, utfallType, Map.of(), apDefinisjoner, VilkårUtfallMerknad.UDEFINERT,
            null, null, null);
    }

    /** Midlertidig Ctor som tar minimum av parametere, og ingen regel evaluering og input data.  Vil heller aldri være overstyrt. */
    public VilkårData(VilkårData generisk, Map<String, Object> merknadParametere, VilkårUtfallMerknad vilkårUtfallMerknad) {
        this(generisk.vilkårType, generisk.utfallType, merknadParametere, generisk.aksjonspunktDefinisjoner, vilkårUtfallMerknad,
            null, null, null);
    }

}
