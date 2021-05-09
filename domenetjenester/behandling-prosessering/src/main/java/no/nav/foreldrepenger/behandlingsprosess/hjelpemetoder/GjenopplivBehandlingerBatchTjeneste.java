package no.nav.foreldrepenger.behandlingsprosess.hjelpemetoder;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchStatus;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.gjenopptak.AutomatiskGjenopptagelseTjeneste;

/**
 * Batchservice som finner alle behandlinger som ikke er aktive og lager en
 * ditto prosess task for hver. Kriterier for gjenoppliving: Behandlingen er
 * ikke avsluttet/iverksatt og det finnes ikke åpne aksjonspunkt eller
 * autopunkt.
 */
@ApplicationScoped
public class GjenopplivBehandlingerBatchTjeneste implements BatchTjeneste {

    static final String BATCHNAME = "BVL097";

    private AutomatiskGjenopptagelseTjeneste automatiskGjenopptagelseTjeneste;

    @Inject
    public GjenopplivBehandlingerBatchTjeneste(AutomatiskGjenopptagelseTjeneste automatiskGjenopptagelseTjeneste) {
        this.automatiskGjenopptagelseTjeneste = automatiskGjenopptagelseTjeneste;
    }

    @Override
    public String launch(BatchArguments arguments) {
        var executionId = BATCHNAME + automatiskGjenopptagelseTjeneste.gjenopplivBehandlinger();
        return executionId;
    }

    @Override
    public BatchStatus status(String executionId) {
        return BatchStatus.OK;
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }

}
