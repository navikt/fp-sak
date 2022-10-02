package no.nav.foreldrepenger.mottak.vedtak.observer;

import java.time.LocalDateTime;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelsemottakRepository;
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
    private static final boolean isProd = Environment.current().isProd();

    private ProsessTaskTjeneste taskTjeneste;
    private HendelsemottakRepository mottakRepository;

    public VedtaksHendelseObserver() {
    }

    @Inject
    public VedtaksHendelseObserver(ProsessTaskTjeneste taskTjeneste,
                                   HendelsemottakRepository mottakRepository) {
        this.taskTjeneste = taskTjeneste;
        this.mottakRepository = mottakRepository;
    }

    public void observerBehandlingVedtakEvent(@Observes BehandlingVedtakEvent event) {
        var behandling = event.behandling();
        if (!event.iverksattVedtak()) {
            return;
        }

        var hendelseId = "FPVEDTAK" + behandling.getUuid().toString();
        if (!mottakRepository.hendelseErNy(hendelseId)) {
            LOG.info("OBSERVER Mottatt vedtakshendelse på nytt hendelse={}", hendelseId);
            return;
        }
        mottakRepository.registrerMottattHendelse(hendelseId);

        opprettTasksForFpsakVedtak(behandling);
    }

    private void opprettTasksForFpsakVedtak(Behandling behandling) {
        var fagsakYtelseType = behandling.getFagsakYtelseType();

        if (!behandling.erYtelseBehandling()) {
            // TFP-4952: legg til behandlingsoppretting + evt flytt VKYtasks for klage + anke fra OpprettProsessTaskIverksett til her
            return;
        }

        if (!VURDER_OVERLAPP.contains(fagsakYtelseType)) {
            return;
        }

        // Unngå gå i beina på på iverksettingstasker med sen respons
        if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
            if (isProd) {
                lagreProsesstaskFor(behandling, TaskType.forProsessTask(StartBerørtBehandlingTask.class), 2);
                lagreProsesstaskFor(behandling, TaskType.forProsessTask(VurderOpphørAvYtelserTask.class), 5);
            } else {
                lagreProsesstaskFor(behandling, TaskType.forProsessTask(StartBerørtBehandlingTask.class), 0);
                lagreProsesstaskFor(behandling, TaskType.forProsessTask(VurderOpphørAvYtelserTask.class), 2);
            }
        } else { // SVP
            lagreProsesstaskFor(behandling, TaskType.forProsessTask(VurderOpphørAvYtelserTask.class), 0);
        }

    }

    void lagreProsesstaskFor(Behandling behandling, TaskType taskType, int delaysecs) {
        var data = ProsessTaskData.forTaskType(taskType);
        data.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        data.setCallId(behandling.getUuid().toString());
        data.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(delaysecs));
        taskTjeneste.lagre(data);
    }
}
