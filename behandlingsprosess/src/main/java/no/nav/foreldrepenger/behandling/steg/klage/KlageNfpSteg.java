package no.nav.foreldrepenger.behandling.steg.klage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

import static java.util.Collections.singletonList;

@BehandlingStegRef(BehandlingStegType.KLAGE_NFP)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class KlageNfpSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    public KlageNfpSteg() {
        // For CDI proxy
    }

    @Inject
    public KlageNfpSteg(BehandlingRepository behandlingRepository, BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var aksjonspunktDefinisjons = singletonList(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP);

        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjons);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
            BehandlingStegType sisteSteg) {
        endreAnsvarligEnhetTilNFPVedTilbakeføringOgLagreHistorikkinnslag(kontekst);
    }

    private void endreAnsvarligEnhetTilNFPVedTilbakeføringOgLagreHistorikkinnslag(BehandlingskontrollKontekst kontekst) {

        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        if (behandling.getBehandlendeEnhet() != null && !BehandlendeEnhetTjeneste.getKlageInstans()
            .enhetId()
            .equals(behandling.getBehandlendeEnhet())) {
            return;
        }
        var tilEnhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(behandling.getFagsak());
        behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, tilEnhet, HistorikkAktør.VEDTAKSLØSNINGEN, "");
    }
}
