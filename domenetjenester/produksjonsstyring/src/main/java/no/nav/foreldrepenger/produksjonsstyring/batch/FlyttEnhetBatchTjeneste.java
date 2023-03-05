package no.nav.foreldrepenger.produksjonsstyring.batch;

import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.task.OppdaterBehandlendeEnhetTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

/**
 * Flytting iht PFP-4662
 */
@ApplicationScoped
public class FlyttEnhetBatchTjeneste implements BatchTjeneste {

    private static final String BATCHNAVN = "BVL061";

    private BehandlingKandidaterRepository behandlingKandidaterRepository;
    private ProsessTaskTjeneste taskTjeneste;

    FlyttEnhetBatchTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FlyttEnhetBatchTjeneste(BehandlingKandidaterRepository behandlingKandidaterRepository,
                                   ProsessTaskTjeneste taskTjeneste) {
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;
        this.taskTjeneste = taskTjeneste;
    }

    @Override
    public FlyttEnhetBatchArguments createArguments(Map<String, String> jobArguments) {
        return new FlyttEnhetBatchArguments(jobArguments);
    }

    @Override
    public String launch(BatchArguments arguments) {
        var flyttEnhetBatchArguments = (FlyttEnhetBatchArguments)arguments;
        var kandidater = behandlingKandidaterRepository.finnBehandlingerIkkeAvsluttetPåAngittEnhet(flyttEnhetBatchArguments.getEnhetId());
        kandidater.forEach(beh -> {
            var taskData = ProsessTaskData.forProsessTask(OppdaterBehandlendeEnhetTask.class);
            taskData.setBehandling(beh.getFagsakId(), beh.getId(), beh.getAktørId().getId());
            taskData.setCallIdFraEksisterende();
            taskTjeneste.lagre(taskData);
        });
        return BATCHNAVN + "-" + UUID.randomUUID();
    }

    @Override
    public String getBatchName() {
        return BATCHNAVN;
    }
}
