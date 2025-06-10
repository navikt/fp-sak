package no.nav.foreldrepenger.dokumentbestiller.observers;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingHenlagtEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

/**
 * Observerer Aksjonspunkt og sender brev ved behov
 */
@ApplicationScoped
public class SendBrevVedHenleggelseObserver {

    private static final Set<BehandlingResultatType> SEND_BREV_VED_HENLEGGELSE = Set.of(
        BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET,
        BehandlingResultatType.HENLAGT_KLAGE_TRUKKET,
        BehandlingResultatType.HENLAGT_INNSYN_TRUKKET
    );

    private BehandlingRepository behandlingRepository;
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;


    public SendBrevVedHenleggelseObserver() {
        //CDI
    }

    @Inject
    public SendBrevVedHenleggelseObserver(BehandlingRepository behandlingRepository,
                                          DokumentBestillerTjeneste dokumentBestillerTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
    }

    public void sendBrevForHenleggelse(@Observes BehandlingHenlagtEvent event) {
        if (SEND_BREV_VED_HENLEGGELSE.contains(event.behandlingResultatType())) {
            var behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
            sendHenleggelsesbrev(behandling);
        }
    }

    private void sendHenleggelsesbrev(Behandling behandling) {
        var dokumentBestilling = DokumentBestilling.builder()
            .medBehandlingUuid(behandling.getUuid())
            .medSaksnummer(behandling.getSaksnummer())
            .medDokumentMal(DokumentMalType.INFO_OM_HENLEGGELSE)
            .build();
        dokumentBestillerTjeneste.bestillDokument(dokumentBestilling);
    }

}
