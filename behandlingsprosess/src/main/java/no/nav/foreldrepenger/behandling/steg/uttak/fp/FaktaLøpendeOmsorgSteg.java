package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.uttak.UttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.fakta.omsorg.AvklarLøpendeOmsorgAksjonspunktUtleder;

@BehandlingStegRef(BehandlingStegType.FAKTA_LØPENDE_OMSORG)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class FaktaLøpendeOmsorgSteg implements UttakSteg {

    private AvklarLøpendeOmsorgAksjonspunktUtleder utleder;
    private UttakInputTjeneste uttakInputTjeneste;

    @Inject
    public FaktaLøpendeOmsorgSteg(AvklarLøpendeOmsorgAksjonspunktUtleder utleder, UttakInputTjeneste uttakInputTjeneste) {
        this.utleder = utleder;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    FaktaLøpendeOmsorgSteg() {
        //CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var uttakInput = uttakInputTjeneste.lagInput(kontekst.getBehandlingId());
        var omsorgAp = utleder.utledAksjonspunktFor(uttakInput);
        return BehandleStegResultat.utførtMedAksjonspunkter(omsorgAp.stream().toList());
    }
}
