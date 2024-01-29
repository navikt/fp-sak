package no.nav.foreldrepenger.mottak.vedtak.observer;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.mottak.vedtak.StartBerørtBehandlingTask;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.VurderOpphørAvYtelserTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ApplicationScoped
public class VedtaksHendelseObserver {

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksHendelseObserver.class);

    private static final Set<FagsakYtelseType> VURDER_OVERLAPP = Set.of(FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.SVANGERSKAPSPENGER);
    private static final Environment ENV = Environment.current();

    private ProsessTaskTjeneste taskTjeneste;
    public VedtaksHendelseObserver() {
    }

    @Inject
    public VedtaksHendelseObserver(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    public void observerBehandlingVedtakEvent(@Observes BehandlingVedtakEvent event) {
        var behandling = event.behandling();
        if (!event.iverksattVedtak()) {
            return;
        }

        var fagsakYtelseType = behandling.getFagsakYtelseType();

        if (!behandling.erYtelseBehandling()) {
            if (BehandlingType.KLAGE.equals(behandling.getType()) || BehandlingType.ANKE.equals(behandling.getType())) {
                lagreProsesstaskFor(behandling, TaskType.forProsessTask(MottaKlageAnkeVedtakTask.class), 1);
            }
            return;
        }

        if (!VURDER_OVERLAPP.contains(fagsakYtelseType)) {
            return;
        }

        // Unngå gå i beina på på iverksettingstasker med sen respons fra OS
        if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
            lagreProsesstaskFor(behandling, TaskType.forProsessTask(StartBerørtBehandlingTask.class), ENV.isLocal() ? 1 : 2);
        }
        lagreProsesstaskFor(behandling, TaskType.forProsessTask(VurderOpphørAvYtelserTask.class), ENV.isLocal() ? 2 : 20); // Kafka kan ta litt tid
    }

    void lagreProsesstaskFor(Behandling behandling, TaskType taskType, int delaysecs) {
        var data = ProsessTaskData.forTaskType(taskType);
        data.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        data.setCallId(behandling.getUuid().toString());
        data.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(delaysecs));
        taskTjeneste.lagre(data);
    }
}
