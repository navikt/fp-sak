package no.nav.foreldrepenger.produksjonsstyring.batch;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchStatus;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.task.OppdaterBehandlendeEnhetTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

/**
 * Flytting iht PFP-4662
 */
@ApplicationScoped
public class FlyttEnhetBatchTjeneste implements BatchTjeneste {

    private static final String BATCHNAVN = "BVL061";

    private BehandlingKandidaterRepository behandlingKandidaterRepository;
    private ProsessTaskRepository prosessTaskRepository;

    FlyttEnhetBatchTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FlyttEnhetBatchTjeneste(BehandlingKandidaterRepository behandlingKandidaterRepository,
                                   ProsessTaskRepository prosessTaskRepository) {
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public FlyttEnhetBatchArguments createArguments(Map<String, String> jobArguments) {
        return new FlyttEnhetBatchArguments(jobArguments);
    }

    @Override
    public String launch(BatchArguments arguments) {
        FlyttEnhetBatchArguments flyttEnhetBatchArguments = (FlyttEnhetBatchArguments)arguments;  // NOSONAR
        List<Behandling> kandidater = behandlingKandidaterRepository.finnBehandlingerIkkeAvsluttetPåAngittEnhet(flyttEnhetBatchArguments.getEnhetId());
        kandidater.forEach(beh -> {
            ProsessTaskData taskData = new ProsessTaskData(OppdaterBehandlendeEnhetTask.TASKTYPE);
            taskData.setBehandling(beh.getFagsakId(), beh.getId(), beh.getAktørId().getId());
            taskData.setCallIdFraEksisterende();
            prosessTaskRepository.lagre(taskData);
        });
        return BATCHNAVN + "-" + UUID.randomUUID();
    }

    @Override
    public BatchStatus status(String batchInstanceNumber) {
        return BatchStatus.OK;
    }

    @Override
    public String getBatchName() {
        return BATCHNAVN;
    }
}
