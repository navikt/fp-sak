package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.uttak.UttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.fakta.KontrollerFaktaUttakTjeneste;

@BehandlingStegRef(BehandlingStegType.KONTROLLER_FAKTA_UTTAK)
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@BehandlingTypeRef
@ApplicationScoped
public class KontrollerFaktaUttakSteg implements UttakSteg {

    private KontrollerFaktaUttakTjeneste kontrollerFaktaUttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private RyddFaktaUttakTjenesteFørstegangsbehandling ryddFaktaUttakTjeneste;

    @Inject
    public KontrollerFaktaUttakSteg(UttakInputTjeneste uttakInputTjeneste,
            KontrollerFaktaUttakTjeneste kontrollerFaktaUttakTjeneste,
            RyddFaktaUttakTjenesteFørstegangsbehandling ryddFaktaUttakTjeneste) {
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.kontrollerFaktaUttakTjeneste = kontrollerFaktaUttakTjeneste;
        this.ryddFaktaUttakTjeneste = ryddFaktaUttakTjeneste;
    }

    KontrollerFaktaUttakSteg() {
        // CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var input = uttakInputTjeneste.lagInput(kontekst.getBehandlingId());
        var aksjonspunktDefinisjonList = kontrollerFaktaUttakTjeneste.utledAksjonspunkter(input);
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
