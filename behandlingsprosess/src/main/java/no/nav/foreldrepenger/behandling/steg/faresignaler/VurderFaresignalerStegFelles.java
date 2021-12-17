package no.nav.foreldrepenger.behandling.steg.faresignaler;

import static java.util.Collections.singletonList;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;

public abstract class VurderFaresignalerStegFelles implements BehandlingSteg {

    private RisikovurderingTjeneste risikovurderingTjeneste;
    private BehandlingRepository behandlingRepository;


    protected VurderFaresignalerStegFelles() {
        // for CDI proxy
    }

    public VurderFaresignalerStegFelles(RisikovurderingTjeneste risikovurderingTjeneste,
                                        BehandlingRepository behandlingRepository) {
        this.risikovurderingTjeneste = risikovurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var referanse = BehandlingReferanse.fra(behandling);
        if (risikovurderingTjeneste.skalVurdereFaresignaler(referanse)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(AksjonspunktDefinisjon.VURDER_FARESIGNALER));
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
