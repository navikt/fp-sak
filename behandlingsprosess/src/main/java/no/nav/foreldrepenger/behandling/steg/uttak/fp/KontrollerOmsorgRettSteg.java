package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.uttak.UttakSteg;
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
@BehandlingTypeRef
@ApplicationScoped
public class KontrollerOmsorgRettSteg implements BehandlingSteg {

    private OmsorgRettUttakTjeneste omsorgRettUttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private RyddFaktaUttakTjenesteFørstegangsbehandling ryddFaktaUttakTjeneste;

    @Inject
    public KontrollerOmsorgRettSteg(UttakInputTjeneste uttakInputTjeneste,
                                    OmsorgRettUttakTjeneste omsorgRettUttakTjeneste,
                                    RyddFaktaUttakTjenesteFørstegangsbehandling ryddFaktaUttakTjeneste) {
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.omsorgRettUttakTjeneste = omsorgRettUttakTjeneste;
        this.ryddFaktaUttakTjeneste = ryddFaktaUttakTjeneste;
    }

    KontrollerOmsorgRettSteg() {
        // CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var input = uttakInputTjeneste.lagInput(kontekst.getBehandlingId());
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(input.getBehandlingReferanse().behandlingType())) {
            omsorgRettUttakTjeneste.avklarOmAnnenForelderHarRett(input.getBehandlingReferanse());
        }
        var aksjonspunktDefinisjonList = omsorgRettUttakTjeneste.utledAksjonspunkter(input);
        var resultater = aksjonspunktDefinisjonList.stream()
                .map(def -> AksjonspunktResultat.opprettForAksjonspunkt(def))
                .collect(Collectors.toList());
        return BehandleStegResultat.utførtMedAksjonspunktResultater(resultater);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
            BehandlingStegType sisteSteg) {
        ryddFaktaUttakTjeneste.ryddVedHoppOverBakover(kontekst);
    }
}
