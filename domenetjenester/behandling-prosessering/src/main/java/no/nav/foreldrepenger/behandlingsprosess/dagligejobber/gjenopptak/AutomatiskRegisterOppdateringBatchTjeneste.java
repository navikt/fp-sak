package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.gjenopptak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchTjeneste;

/**
 * Batchservice som finner alle behandlinger som skal gjenopptas, og lager en
 * ditto prosess task for hver. Kriterier for gjenopptagelse: Behandlingen har
 * et åpent aksjonspunkt som er et autopunkt og har en frist som er passert.
 */
@ApplicationScoped
public class AutomatiskRegisterOppdateringBatchTjeneste implements BatchTjeneste {

    static final String BATCHNAME = "BVL007";

    private AutomatiskGjenopptagelseTjeneste automatiskGjenopptagelseTjeneste;

    @Inject
    public AutomatiskRegisterOppdateringBatchTjeneste(AutomatiskGjenopptagelseTjeneste automatiskGjenopptagelseTjeneste) {
        this.automatiskGjenopptagelseTjeneste = automatiskGjenopptagelseTjeneste;
    }

    @Override
    public String launch(BatchArguments arguments) {
        var executionId = BATCHNAME + automatiskGjenopptagelseTjeneste.oppdaterBehandlingerFraOppgaveFrist();
        return executionId;
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }
}
