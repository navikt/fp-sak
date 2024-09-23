package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.medlemskap.es;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårStegImpl;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;
import no.nav.foreldrepenger.konfig.Environment;

@BehandlingStegRef(BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
@ApplicationScoped
public class VurderMedlemskapvilkårStegImpl extends InngangsvilkårStegImpl {

    private static final Environment ENV = Environment.current();
    private static final List<VilkårType> STØTTEDE_VILKÅR = List.of(VilkårType.MEDLEMSKAPSVILKÅRET);

    @Inject
    public VurderMedlemskapvilkårStegImpl(BehandlingRepositoryProvider repositoryProvider,
                                          InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);
    }

    @Override
    public List<VilkårType> vilkårHåndtertAvSteg() {
        return STØTTEDE_VILKÅR;
    }

    @Override
    protected boolean manuellVurderingNårRegelVilkårIkkeOppfylt() {
        return !ENV.isProd();
    }

    @Override
    protected BehandleStegResultat stegResultatVilkårIkkeOppfylt(RegelResultat regelResultat, Behandling behandling) {
        if (ENV.isProd()) {
            return super.stegResultatVilkårIkkeOppfylt(regelResultat, behandling);
        } else {
            return BehandleStegResultat.utførtMedAksjonspunkt(AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET);
        }
    }
}
