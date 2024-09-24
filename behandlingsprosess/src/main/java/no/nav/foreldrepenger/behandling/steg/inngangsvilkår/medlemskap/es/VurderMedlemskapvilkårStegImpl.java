package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.medlemskap.es;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårStegImpl;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;

@BehandlingStegRef(BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
@ApplicationScoped
public class VurderMedlemskapvilkårStegImpl extends InngangsvilkårStegImpl {

    private static final List<VilkårType> STØTTEDE_VILKÅR = List.of(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE);

    private Medlemsvilkårutleder medlemsvilkårutleder;

    @Inject
    public VurderMedlemskapvilkårStegImpl(BehandlingRepositoryProvider repositoryProvider,
                                          InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste,
                                          Medlemsvilkårutleder medlemsvilkårutleder) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);
        this.medlemsvilkårutleder = medlemsvilkårutleder;
    }

    @Override
    public List<VilkårType> vilkårHåndtertAvSteg() {
        return STØTTEDE_VILKÅR;
    }

    @Override
    protected void opprettDynamiskeVilkårForBehandling(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        medlemsvilkårutleder.opprettVilkårForBehandling(kontekst, behandling);
    }

    @Override
    protected boolean manuellVurderingNårRegelVilkårIkkeOppfylt() {
        return true;
    }

    @Override
    protected BehandleStegResultat stegResultatVilkårIkkeOppfylt(RegelResultat regelResultat, Behandling behandling) {
        var aksjonspunkt = regelResultat.vilkårResultat().getVilkårTyper().contains(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE) ?
            AksjonspunktDefinisjon.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR : AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET;
        return BehandleStegResultat.utførtMedAksjonspunkt(aksjonspunkt);
    }
}
