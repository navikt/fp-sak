package no.nav.foreldrepenger.poststed;

import java.util.Properties;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.batch.BatchTjeneste;
/**
 * Henter ned offisielle kodeverk fra Nav som brukes i løsningen og synker den til egen kodeverk-tabell.
 */
@ApplicationScoped
public class PoststedBatchTjeneste implements BatchTjeneste {

    private static final String BATCHNAVN = "BVL005";

    private PostnummerSynkroniseringTjeneste kodeverkSynkronisering;

    PoststedBatchTjeneste() {
        // for CDI proxy
    }

    @Inject
    public PoststedBatchTjeneste(PostnummerSynkroniseringTjeneste kodeverkSynkronisering) {
        this.kodeverkSynkronisering = kodeverkSynkronisering;
    }

    @Override
    public String launch(Properties properties) {
        kodeverkSynkronisering.synkroniserPostnummer();
        return BATCHNAVN + "-" + UUID.randomUUID();
    }

    @Override
    public String getBatchName() {
        return BATCHNAVN;
    }
}
