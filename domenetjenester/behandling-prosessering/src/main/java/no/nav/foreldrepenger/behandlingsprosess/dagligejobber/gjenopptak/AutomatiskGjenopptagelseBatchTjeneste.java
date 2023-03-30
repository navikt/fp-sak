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
public class AutomatiskGjenopptagelseBatchTjeneste implements BatchTjeneste {

    static final String BATCHNAME = "BVL004";

    private AutomatiskGjenopptagelseTjeneste automatiskGjenopptagelseTjeneste;

    @Inject
    public AutomatiskGjenopptagelseBatchTjeneste(AutomatiskGjenopptagelseTjeneste automatiskGjenopptagelseTjeneste) {
        this.automatiskGjenopptagelseTjeneste = automatiskGjenopptagelseTjeneste;
    }

    @Override
    public String launch(BatchArguments arguments) {
        return BATCHNAME + automatiskGjenopptagelseTjeneste.gjenopptaBehandlinger();
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }
}
