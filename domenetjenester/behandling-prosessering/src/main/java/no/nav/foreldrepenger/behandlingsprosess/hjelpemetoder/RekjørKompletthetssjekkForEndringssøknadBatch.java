package no.nav.foreldrepenger.behandlingsprosess.hjelpemetoder;

import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchStatus;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.GjenopptaBehandlingTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.log.mdc.MDCOperations;

/**
 * Midlertidig batch for å rekjøre kompletthetssjekk for endringssøknader som venter på IM
 *
 * Skal kjøre en gang
 *
 */

@ApplicationScoped
class RekjørKompletthetssjekkForEndringssøknadBatch implements BatchTjeneste {

    private static final String BATCHNAME = "BVL033";

    private BehandlingKandidaterRepository behandlingKandidaterRepository;
    private ProsessTaskRepository prosessTaskRepository;

    @Inject
    public RekjørKompletthetssjekkForEndringssøknadBatch(BehandlingKandidaterRepository behandlingKandidaterRepository,
                                                         ProsessTaskRepository prosessTaskRepository) {
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public String launch(BatchArguments arguments) {
        List<Behandling> behandlinger = behandlingKandidaterRepository.finnRevurderingerPåVentIKompletthet();

        String callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";

        for (Behandling behandling : behandlinger) {
            opprettRekjøringsTask(behandling, callId);
        }

        return BATCHNAME + "-" + UUID.randomUUID();
    }

    private void opprettRekjøringsTask(Behandling behandling, String callId) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(GjenopptaBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        // unik per task da det er ulike tasks for hver behandling
        String nyCallId = callId + behandling.getId();
        prosessTaskData.setCallId(nyCallId);

        prosessTaskRepository.lagre(prosessTaskData);
    }

    @Override
    public BatchStatus status(String batchInstanceNumber) {
        // Antar her at alt har gått bra siden denne er en synkron jobb.
        return BatchStatus.OK;
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }
}
