package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

/**
 * Batchservice som finner alle behandlinger som skal gjenopptas, og lager en
 * ditto prosess task for hver. Kriterier for gjenopptagelse: Behandlingen har
 * et åpent aksjonspunkt som er et autopunkt og har en frist som er passert.
 */
@ApplicationScoped
public class AutomatiskArenaReguleringBatchTjeneste implements BatchTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(AutomatiskArenaReguleringBatchTjeneste.class);
    static final String BATCHNAME = "BVL072";
    private static final String EXECUTION_ID_SEPARATOR = "-";

    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private ProsessTaskTjeneste taskTjeneste;

    @Inject
    public AutomatiskArenaReguleringBatchTjeneste(BehandlingRepositoryProvider repositoryProvider,
            ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
        this.behandlingRevurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
    }

    @Override
    public AutomatiskArenaReguleringBatchArguments createArguments(Map<String, String> jobArguments) {
        return new AutomatiskArenaReguleringBatchArguments(jobArguments);
    }

    @Override
    public String launch(BatchArguments arguments) {
        var batchArguments = (AutomatiskArenaReguleringBatchArguments) arguments;
        var executionId = BATCHNAME + EXECUTION_ID_SEPARATOR;

        var tilVurdering = hentKandidater(batchArguments);

        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        final var callId = MDCOperations.getCallId() + "_";
        if (batchArguments.getSkalRevurdere()) {
            tilVurdering.forEach(sak -> opprettReguleringTask(sak.fagsakId(), sak.aktørId(), callId));
        } else {
            tilVurdering.forEach(sak -> LOG.info("Skal revurdere sak {}", sak.fagsakId()));
        }
        return executionId + tilVurdering.size();
    }

    List<BehandlingRevurderingRepository.FagsakIdAktørId> hentKandidater(AutomatiskArenaReguleringBatchArguments batchArguments) {
        return behandlingRevurderingRepository.finnSakerMedBehovForArenaRegulering(AutomatiskArenaReguleringBatchArguments.DATO, batchArguments.getSatsDato());
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }

    private void opprettReguleringTask(Long fagsakId, AktørId aktørId, String callId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskGrunnbelopReguleringTask.class);
        prosessTaskData.setFagsak(fagsakId, aktørId.getId());
        prosessTaskData.setCallId(callId + fagsakId);
        prosessTaskData.setPrioritet(100);
        taskTjeneste.lagre(prosessTaskData);
    }
}
