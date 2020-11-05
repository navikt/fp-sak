package no.nav.foreldrepenger.poststed;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchStatus;
import no.nav.foreldrepenger.batch.BatchTjeneste;
/**
 * Henter ned offisielle kodeverk fra NAV som brukes i løsningen og synker den til egen kodeverk-tabell.
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
    public String launch(BatchArguments arguments) {
        kodeverkSynkronisering.synkroniserPostnummer();
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
