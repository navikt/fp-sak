package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.felles;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderBekreftetOpptjening;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderOppgittOpptjening;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsVilkårTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

public abstract class OpptjeningFaktaStegFelles implements BehandlingSteg {

    private BehandlingRepositoryProvider repositoryProvider;
    private AksjonspunktutlederForVurderOppgittOpptjening aksjonspunktutlederOppgitt;
    private AksjonspunktutlederForVurderBekreftetOpptjening aksjonspunktutlederBekreftet;
    private OpptjeningsVilkårTjeneste opptjeningsVilkårTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    protected OpptjeningFaktaStegFelles() {
        // CDI proxy
    }

    protected OpptjeningFaktaStegFelles(BehandlingRepositoryProvider repositoryProvider,
            AksjonspunktutlederForVurderBekreftetOpptjening aksjonspunktutlederBekreftet,
            AksjonspunktutlederForVurderOppgittOpptjening aksjonspunktutlederOppgitt,
            OpptjeningsVilkårTjeneste opptjeningsVilkårTjeneste,
            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.repositoryProvider = repositoryProvider;
        this.aksjonspunktutlederBekreftet = aksjonspunktutlederBekreftet;
        this.aksjonspunktutlederOppgitt = aksjonspunktutlederOppgitt;
        this.opptjeningsVilkårTjeneste = opptjeningsVilkårTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = repositoryProvider.getBehandlingRepository().hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);
        var resultat = opptjeningsVilkårTjeneste.vurderOpptjeningsVilkår(ref);
        if (VilkårUtfallType.OPPFYLT.equals(resultat.utfallType())) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);

        var resultatOppgitt = aksjonspunktutlederOppgitt.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref, stp));
        if (!resultatOppgitt.isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(resultatOppgitt);
        }

        var resultatRegister = aksjonspunktutlederBekreftet.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref, stp));
        return BehandleStegResultat.utførtMedAksjonspunktResultater(resultatRegister);
    }
}
