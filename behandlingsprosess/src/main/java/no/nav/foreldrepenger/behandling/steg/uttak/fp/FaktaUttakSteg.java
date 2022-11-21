package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.uttak.UttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.fakta.v2.FaktaUttakAksjonspunktUtleder;

@BehandlingStegRef(BehandlingStegType.FAKTA_UTTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class FaktaUttakSteg implements UttakSteg {

    private FaktaUttakAksjonspunktUtleder faktaUttakAksjonspunktUtleder;
    private UttakInputTjeneste uttakInputTjeneste;

    @Inject
    public FaktaUttakSteg(FaktaUttakAksjonspunktUtleder faktaUttakAksjonspunktUtleder,
                          UttakInputTjeneste uttakInputTjeneste) {
        this.faktaUttakAksjonspunktUtleder = faktaUttakAksjonspunktUtleder;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    FaktaUttakSteg() {
        //CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var uttakInput = uttakInputTjeneste.lagInput(kontekst.getBehandlingId());
        var faktaUttakAP = faktaUttakAksjonspunktUtleder.utledAksjonspunkterFor(uttakInput);
        return BehandleStegResultat.utførtMedAksjonspunkter(faktaUttakAP);
    }
}
