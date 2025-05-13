package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.events.MottattDokumentPersistertEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.SakensPersonerEndretEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
public class BehandlendeEnhetObserver {

    private BehandlingRepository behandlingRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    public BehandlendeEnhetObserver() {
        //Cool Devices Installed
    }

    @Inject
    public BehandlendeEnhetObserver(BehandlingRepository behandlingRepository, BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    public void observerPersongalleriEvent(@Observes SakensPersonerEndretEvent event) {
        var behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
        oppdaterEnhetVedBehov(behandling, event.årsak());
    }

    public void observerMottattDokumentPersistert(@Observes MottattDokumentPersistertEvent event) {
        // Lagre behandling med rettighetsdato o.l.
        if (event.getMottattDokument().getDokumentType().erSøknadType()) {
            oppdaterEnhetVedBehov(event.behandling(), "Søknad");
        }
    }

    private void oppdaterEnhetVedBehov(Behandling behandling, String årsak) {
        behandlendeEnhetTjeneste.sjekkEnhetEtterEndring(behandling)
            .ifPresent(enhet -> behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, enhet, HistorikkAktør.VEDTAKSLØSNINGEN, årsak));
    }


}
