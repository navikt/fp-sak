package no.nav.foreldrepenger.inngangsvilkaar.søknad;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;

@ApplicationScoped
@VilkårTypeRef(VilkårType.SØKERSOPPLYSNINGSPLIKT)
public class InngangsvilkårSøkersOpplysningsplikt implements Inngangsvilkår {

    private Kompletthetsjekker kompletthetsjekker;

    public InngangsvilkårSøkersOpplysningsplikt() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårSøkersOpplysningsplikt(Kompletthetsjekker kompletthetsjekker) {
        this.kompletthetsjekker = kompletthetsjekker;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        return vurderOpplysningspliktOppfyltAutomatisk(ref);
    }

    private VilkårData vurderOpplysningspliktOppfyltAutomatisk(BehandlingReferanse ref) {
        var oppfylt = new VilkårData(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT, List.of());

        var manuellVurdering = new VilkårData(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.IKKE_VURDERT,
            List.of(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU));

        var ytelseType = ref.fagsakYtelseType();
        var behandlingType = ref.behandlingType();
        if (BehandlingType.REVURDERING.equals(behandlingType)) {
            // For revurdering skal det ikke utføres vilkårskontroll om opplysningsplikt (NOOP)
            return oppfylt;
        }

        var søknadKomplett = this.kompletthetsjekker.erForsendelsesgrunnlagKomplett(ref);
        if (søknadKomplett) {
            return oppfylt;
        }

        return manuellVurdering;
    }
}

