package no.nav.foreldrepenger.web.app.tjenester;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.registerinnhenting.task.InnhentIAYIAbakusTask;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

public class VurderProsessTaskStatusForPollingApi {
    private static final Logger LOG = LoggerFactory.getLogger(VurderProsessTaskStatusForPollingApi.class);

    private Long entityId;

    public VurderProsessTaskStatusForPollingApi(Long entityId) {
        this.entityId = entityId;
    }

    public Optional<AsyncPollingStatus> sjekkStatusNesteProsessTask(String gruppe, Map<String, ProsessTaskData> nesteTask) {
        var maksTidFørNesteKjøring = LocalDateTime.now().plusMinutes(2);
        nesteTask = nesteTask.entrySet().stream()
            .filter(e -> !e.getValue().getStatus().erKjørt()) // trenger ikke FERDIG
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!nesteTask.isEmpty()) {
            var optTask = Optional.ofNullable(nesteTask.get(gruppe));
            if (optTask.isEmpty()) {
                // plukker neste til å polle på
                optTask = nesteTask.values().stream()
                    .findFirst();
            }

            if (optTask.isPresent()) {
                return sjekkStatus(maksTidFørNesteKjøring, optTask.get());
            }
        }
        return Optional.empty();
    }

    private Optional<AsyncPollingStatus> sjekkStatus(LocalDateTime maksTidFørNesteKjøring, ProsessTaskData task) {
        var gruppe = task.getGruppe();
        var callId = task.getPropertyValue("callId");
        var taskStatus = task.getStatus();
        if (ProsessTaskStatus.KLAR.equals(taskStatus) || ProsessTaskStatus.VENTER_SVAR.equals(taskStatus) && TaskType.forProsessTask(
            InnhentIAYIAbakusTask.class).equals(task.taskType()) && task.getNesteKjøringEtter().isBefore(maksTidFørNesteKjøring)) {
            return ventPåKlar(gruppe, maksTidFørNesteKjøring, task, callId);
        }
        if (ProsessTaskStatus.VENTER_SVAR.equals(taskStatus)) {
            return ventPåSvar(gruppe, task, callId);
        }
        // dekker SUSPENDERT, FEILET, VETO
        return håndterFeil(gruppe, task, callId);
    }

    private Optional<AsyncPollingStatus> håndterFeil(String gruppe, ProsessTaskData task, String callId) {
        LOG.info("FP-193308 [{}]. Forespørsel på fagsak [{}] som ikke kan fortsette, Problemer med task gruppe [{}]. Siste prosesstask[{}] status={}",
            callId, entityId, gruppe, task.getId(), task.getStatus());

        var status = new AsyncPollingStatus(AsyncPollingStatus.Status.HALTED, task.getSisteFeil(), null);
        return Optional.of(status);// fortsett å polle på gruppe, er ikke ferdig.
    }

    private Optional<AsyncPollingStatus> ventPåSvar(String gruppe, ProsessTaskData task, String callId) {
        var feilmelding = String.format("FP-193308 [%1$s]. Forespørsel på behandling [id=%2$s] som venter på svar fra annet system (task [id=%4$s], gruppe [%3$s] kjøres ikke før det er mottatt). Task status=%5$s",
            callId, entityId, gruppe, task.getId(), task.getStatus());
        LOG.info(feilmelding);

        var status = new AsyncPollingStatus(AsyncPollingStatus.Status.DELAYED, feilmelding, task.getNesteKjøringEtter());

        return Optional.of(status);// er ikke ferdig, men ok å videresende til visning av behandling med feilmelding der.
    }

    private Optional<AsyncPollingStatus> ventPåKlar(String gruppe, LocalDateTime maksTidFørNesteKjøring, ProsessTaskData task, String callId) {
        if (task.getNesteKjøringEtter().isBefore(maksTidFørNesteKjøring)) {
            var feilmelding = "Venter på prosesstask [" + task.taskType().value() + "][id: " + task.getId() + "]";

            var status = new AsyncPollingStatus(AsyncPollingStatus.Status.PENDING, feilmelding, task.getNesteKjøringEtter(), 500L);

            return Optional.of(status);// fortsett å polle på gruppe, er ikke ferdig.
        }
        var feilmelding = String.format("FP-193309 [%1$s]. Forespørsel på fagsak [id=%2$s] som er utsatt i påvente av task [id=%4$s], Gruppe [%3$s] kjøres ikke før senere. Task status=%5$s, planlagt neste kjøring=%6$s",
           callId, entityId, gruppe, task.getId(), task.getStatus(), task.getNesteKjøringEtter());
        LOG.info(feilmelding);

        var status = new AsyncPollingStatus(AsyncPollingStatus.Status.DELAYED, feilmelding, task.getNesteKjøringEtter());

        return Optional.of(status);// er ikke ferdig, men ok å videresende til visning av behandling med feilmelding der.
    }

}
