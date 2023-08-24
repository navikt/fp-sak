package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.uttak.UttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

import java.util.List;

@BehandlingStegRef(BehandlingStegType.FAKTA_UTTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class FaktaUttakSteg implements UttakSteg {

    private FaktaUttakAksjonspunktUtleder faktaUttakAksjonspunktUtleder;
    private UttakInputTjeneste uttakInputTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public FaktaUttakSteg(FaktaUttakAksjonspunktUtleder faktaUttakAksjonspunktUtleder,
                          UttakInputTjeneste uttakInputTjeneste,
                          BehandlingRepository behandlingRepository) {
        this.faktaUttakAksjonspunktUtleder = faktaUttakAksjonspunktUtleder;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    FaktaUttakSteg() {
        //CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var uttakInput = uttakInputTjeneste.lagInput(kontekst.getBehandlingId());
        var faktaUttakAP = utledAp(uttakInput);
        return BehandleStegResultat.utførtMedAksjonspunkter(faktaUttakAP);
    }

    private List<AksjonspunktDefinisjon> utledAp(UttakInput uttakInput) {
        var behandlingId = uttakInput.getBehandlingReferanse().behandlingId();
        if (harÅpentOverstyringAp(behandlingId)) {
            return List.of();
        }
        //Bruker justert her for å reutlede avbrutt AP ved tilbakehopp
        return faktaUttakAksjonspunktUtleder.utledAksjonspunkterFor(uttakInput);
    }

    private boolean harÅpentOverstyringAp(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId)
            .harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.OVERSTYRING_FAKTA_UTTAK);
    }
}
