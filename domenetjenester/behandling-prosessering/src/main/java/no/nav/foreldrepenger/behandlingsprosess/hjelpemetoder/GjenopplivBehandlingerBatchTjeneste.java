package no.nav.foreldrepenger.behandlingsprosess.hjelpemetoder;

import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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

    static final String BATCHNAME = "BVL007";

    private AutomatiskGjenopptagelseTjeneste automatiskGjenopptagelseTjeneste;

    @Inject
    public GjenopplivBehandlingerBatchTjeneste(AutomatiskGjenopptagelseTjeneste automatiskGjenopptagelseTjeneste) {
        this.automatiskGjenopptagelseTjeneste = automatiskGjenopptagelseTjeneste;
    }

    @Override
    public String launch(Properties properties) {
        return BATCHNAME + automatiskGjenopptagelseTjeneste.gjenopplivBehandlinger();
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }

}
