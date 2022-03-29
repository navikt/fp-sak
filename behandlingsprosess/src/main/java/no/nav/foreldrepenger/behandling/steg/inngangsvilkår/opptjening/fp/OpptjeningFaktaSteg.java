package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.felles.OpptjeningFaktaStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegKoder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderBekreftetOpptjening;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderOppgittOpptjening;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsVilkårTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

/**
 * Steg 81 - Kontroller fakta for opptjening
 */
@BehandlingStegRef(kode = BehandlingStegKoder.VURDER_OPPTJENING_FAKTA_KODE)
@BehandlingTypeRef
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class OpptjeningFaktaSteg extends OpptjeningFaktaStegFelles {

    @Inject
    public OpptjeningFaktaSteg(BehandlingRepositoryProvider repositoryProvider,
            AksjonspunktutlederForVurderBekreftetOpptjening aksjonspunktutlederBekreftet,
            AksjonspunktutlederForVurderOppgittOpptjening aksjonspunktutlederOppgitt,
            @FagsakYtelseTypeRef("FP") OpptjeningsVilkårTjeneste opptjeningsVilkårTjeneste,
            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        super(repositoryProvider, aksjonspunktutlederBekreftet, aksjonspunktutlederOppgitt, opptjeningsVilkårTjeneste, skjæringstidspunktTjeneste);
    }
}
