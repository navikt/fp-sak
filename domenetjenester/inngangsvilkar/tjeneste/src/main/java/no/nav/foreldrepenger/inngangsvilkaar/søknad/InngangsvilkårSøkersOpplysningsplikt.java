package no.nav.foreldrepenger.inngangsvilkaar.søknad;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;

@ApplicationScoped
@VilkårTypeRef(VilkårType.SØKERSOPPLYSNINGSPLIKT)
public class InngangsvilkårSøkersOpplysningsplikt implements Inngangsvilkår {

    private KompletthetsjekkerProvider kompletthetsjekkerProvider;

    public InngangsvilkårSøkersOpplysningsplikt() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårSøkersOpplysningsplikt(KompletthetsjekkerProvider kompletthetsjekkerProvider) {
        this.kompletthetsjekkerProvider = kompletthetsjekkerProvider;
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
        if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType) &&
            BehandlingType.REVURDERING.equals(behandlingType)) {
            // For revurdering FP skal det ikke utføres vilkårskontroll om opplysningsplikt (NOOP)
            return oppfylt;
        }

        var søknadKomplett = this.kompletthetsjekkerProvider.finnKompletthetsjekkerFor(ytelseType, behandlingType).erForsendelsesgrunnlagKomplett(ref);
        if (søknadKomplett) {
            return oppfylt;
        }

        return manuellVurdering;
    }
}

