package no.nav.foreldrepenger.inngangsvilkaar.søknad;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;

@ApplicationScoped
@VilkårTypeRef(VilkårType.SØKERSOPPLYSNINGSPLIKT)
public class InngangsvilkårSøkersOpplysningsplikt implements Inngangsvilkår {


    public InngangsvilkårSøkersOpplysningsplikt() {
        // for CDI proxy
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        return vurderOpplysningspliktOppfyltAutomatisk();
    }

    private VilkårData vurderOpplysningspliktOppfyltAutomatisk() {
        return new VilkårData(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT, List.of());

    }
}

