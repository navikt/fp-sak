package no.nav.foreldrepenger.dokumentbestiller.forlengelsesbrev.batch;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.forlengelsesbrev.tjeneste.SendForlengelsesbrevTjeneste;

/**
 * Henter ut åpne behandlinger der behandlingsfrist er utløpt,
 * sender informasjonsbrev om forlenget behandlingstid og oppdaterer behandlingsfristen.
 */

@ApplicationScoped
public class SendForlengelsesbrevBatchTjeneste implements BatchTjeneste {

    private static final String BATCHNAME = "BVL003";
    private SendForlengelsesbrevTjeneste tjeneste;

    @Inject
    public SendForlengelsesbrevBatchTjeneste(SendForlengelsesbrevTjeneste tjeneste) {
        this.tjeneste = tjeneste;
    }

    @Override
    public String launch(BatchArguments arguments) {
        final var gruppe = tjeneste.sendForlengelsesbrev();
        return BATCHNAME + "-" + gruppe;
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }
}
