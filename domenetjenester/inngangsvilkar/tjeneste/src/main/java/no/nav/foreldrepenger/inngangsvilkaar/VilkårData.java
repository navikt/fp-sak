package no.nav.foreldrepenger.inngangsvilkaar;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

import java.util.List;

public record VilkårData(VilkårType vilkårType, VilkårUtfallType utfallType, VilkårUtfallMerknad vilkårUtfallMerknad,
            List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner, String regelEvaluering, String regelInput, Object ekstraVilkårresultat) {

    /** Ctor som tar minimum av parametere, og ingen regel evaluering og input data.  Vil heller aldri være overstyrt. */
    public VilkårData(VilkårType vilkårType, VilkårUtfallType utfallType, List<AksjonspunktDefinisjon> apDefinisjoner) {
        this(vilkårType, utfallType, VilkårUtfallMerknad.UDEFINERT, apDefinisjoner, null, null, null);
    }

}
