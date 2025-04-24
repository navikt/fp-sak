package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;

import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

/**
 * Observerer relevante status endringer på behandling og fagsak og varsler til Sakogbehandling (eksternt system).
 *
 */
@ApplicationScoped
public class OppdaterSakOgBehandlingEventObserver {

    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste taskTjeneste;

    static final String FORELDREPENGER_SAKSTEMA = Tema.FOR.getOffisiellKode();

    @Inject
    public OppdaterSakOgBehandlingEventObserver(BehandlingRepositoryProvider repositoryProvider,
                                                ProsessTaskTjeneste taskTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.taskTjeneste = taskTjeneste;
    }

    public void observerBehandlingStatus(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        oppdaterSakOgBehandlingVedBehandlingsstatusEndring(event);
    }

    public void observerBehandlingStatus(@Observes BehandlingStatusEvent.BehandlingOpprettetEvent event) {
        oppdaterSakOgBehandlingVedBehandlingsstatusEndring(event);
    }

    /**
     * Sender melding til køen som systemet "Sak og behandling" lytter på
     */
    private void oppdaterSakOgBehandlingVedBehandlingsstatusEndring(BehandlingStatusEvent event) {

        var nyStatus = event.getNyStatus();

        var behandling = behandlingRepository.hentBehandling(event.getBehandlingId());

        sendMeldingTilSakOgBehandling(behandling, nyStatus);

    }

    private void sendMeldingTilSakOgBehandling(Behandling behandling, BehandlingStatus nyStatus) {
        if (FagsakYtelseType.UDEFINERT.equals(behandling.getFagsakYtelseType())) {
            throw new IllegalStateException("Utviklerfeil: Finner ikke behandlingstema for fagsak");
        }
        var behandlingRef = String.format("%s_%s", Fagsystem.FPSAK.getOffisiellKode(), behandling.getId());
        var prosessTaskData = ProsessTaskData.forProsessTask(OppdaterPersonoversiktTask.class);
        prosessTaskData.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
        prosessTaskData.setProperty(OppdaterPersonoversiktTask.PH_REF_KEY, behandlingRef);
        prosessTaskData.setProperty(OppdaterPersonoversiktTask.PH_STATUS_KEY, nyStatus.getKode());
        prosessTaskData.setProperty(OppdaterPersonoversiktTask.PH_TID_KEY, LocalDateTime.now().toString());
        prosessTaskData.setProperty(OppdaterPersonoversiktTask.PH_TYPE_KEY, behandling.getType().getKode());
        taskTjeneste.lagre(prosessTaskData);
    }
}
