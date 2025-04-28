package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.fakta.OmsorgRettUttakTjeneste;

@BehandlingStegRef(BehandlingStegType.KONTROLLER_OMSORG_RETT)
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
@ApplicationScoped
public class KontrollerOmsorgRettFørstegangsbehandlingSteg implements BehandlingSteg {

    private OmsorgRettUttakTjeneste omsorgRettUttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private RyddOmsorgRettTjeneste ryddOmsorgRettTjeneste;

    @Inject
    public KontrollerOmsorgRettFørstegangsbehandlingSteg(UttakInputTjeneste uttakInputTjeneste,
                                                         OmsorgRettUttakTjeneste omsorgRettUttakTjeneste,
                                                         RyddOmsorgRettTjeneste ryddOmsorgRettTjeneste) {
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.omsorgRettUttakTjeneste = omsorgRettUttakTjeneste;
        this.ryddOmsorgRettTjeneste = ryddOmsorgRettTjeneste;
    }

    KontrollerOmsorgRettFørstegangsbehandlingSteg() {
        // CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var input = uttakInputTjeneste.lagInput(kontekst.getBehandlingId());
        omsorgRettUttakTjeneste.avklarOmAnnenForelderHarRett(input.getBehandlingReferanse());
        var aksjonspunktDefinisjonList = omsorgRettUttakTjeneste.utledAksjonspunkter(input);
        var resultater = aksjonspunktDefinisjonList.stream()
                .map(AksjonspunktResultat::opprettForAksjonspunkt)
                .toList();
        return BehandleStegResultat.utførtMedAksjonspunktResultater(resultater);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg, BehandlingStegType sisteSteg) {
        ryddOmsorgRettTjeneste.ryddVedHoppOverBakover(kontekst);
    }
}
