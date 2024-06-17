package no.nav.foreldrepenger.domene.vedtak.batch;

import java.time.LocalDate;
import java.util.Properties;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.batch.BatchTjeneste;

/**
 * Henter ut løpende Fagsaker og avslutter dem hvis det ikke er noen åpne behandlinger
 * og alle perioden for ytelsesvedtaket er passert
 * <p>
 * Skal kjøre en gang i døgnet
 * <p>
 * Kan kjøres med parameter date=<Datoen man vil batchen skal kjøres for(på format dd-MM-yyyy)>
 */

@ApplicationScoped
public class AutomatiskFagsakAvslutningBatchTjeneste implements BatchTjeneste {

    private static final String BATCHNAME = "BVL006";
    private AutomatiskFagsakAvslutningTjeneste automatiskFagsakAvslutningTjeneste;

    @Inject
    public AutomatiskFagsakAvslutningBatchTjeneste(AutomatiskFagsakAvslutningTjeneste automatiskFagsakAvslutningTjeneste) {
        this.automatiskFagsakAvslutningTjeneste = automatiskFagsakAvslutningTjeneste;
    }

    @Override
    public String launch(Properties properties) {
        var avsluttFagsakGruppe = automatiskFagsakAvslutningTjeneste.avsluttFagsaker(BATCHNAME, LocalDate.now());
        return BATCHNAME + "-" + (avsluttFagsakGruppe != null ? avsluttFagsakGruppe : UUID.randomUUID().toString());
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }

}

