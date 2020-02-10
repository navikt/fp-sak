package no.nav.foreldrepenger.behandling.steg.klage;

import static java.util.Collections.singletonList;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@BehandlingStegRef(kode = "KLAGEUI")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class KlageNfpSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private KlageRepository klageRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    public KlageNfpSteg(){
        // For CDI proxy
    }

    @Inject
    public KlageNfpSteg(BehandlingRepository behandlingRepository, 
                        KlageRepository klageRepository,
                        BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.klageRepository = klageRepository;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        List<AksjonspunktDefinisjon> aksjonspunktDefinisjons = singletonList(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP);

        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjons);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
                                   BehandlingStegType sisteSteg) {
        if(!Objects.equals(BehandlingStegType.FATTE_VEDTAK, sisteSteg)) {
            klageRepository.slettKlageVurderingResultat(kontekst.getBehandlingId(), KlageVurdertAv.NFP);
            endreAnsvarligEnhetTilNFPVedTilbakeføringOgLagreHistorikkinnslag(kontekst);
        }
    }

    private void endreAnsvarligEnhetTilNFPVedTilbakeføringOgLagreHistorikkinnslag(BehandlingskontrollKontekst kontekst) {

        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        Behandling sisteFørstegangsbehandling = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(kontekst.getFagsakId(),
            BehandlingType.FØRSTEGANGSSØKNAD).orElseThrow(() -> new IllegalStateException("Fant ingen behandling som passet for saksnummer: "
            + behandling.getFagsak().getSaksnummer()));
        if (behandling.getBehandlendeEnhet() != null && behandling.getBehandlendeEnhet().equals(sisteFørstegangsbehandling.getBehandlendeEnhet())) {
            return;
        }
        OrganisasjonsEnhet tilEnhet = behandlendeEnhetTjeneste.sjekkEnhetVedNyAvledetBehandling(sisteFørstegangsbehandling).orElse(sisteFørstegangsbehandling.getBehandlendeOrganisasjonsEnhet());
        behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, tilEnhet, HistorikkAktør.VEDTAKSLØSNINGEN, "");
    }
}
