package no.nav.foreldrepenger.batch;

import java.util.Map;

public interface BatchTjeneste {

    /**
     * Syntaktisk evalurering av job argumentene
     *
     * @param jobArguments map med argument verdier
     * @return argumentene transformert over til batch-tjenestenes implementasjon av argumenter
     * @throws no.nav.foreldrepenger.batch.feil.UnknownArgumentsReceivedVLBatchException if jobArguments contains any unknown keys
     */
    default BatchArguments createArguments(Map<String, String> jobArguments) {
        return new EmptyBatchArguments(jobArguments);
    }

    /**
     * Launches a batch.
     *
     * @param arguments job arguments
     * @return unique executionId. Er sammensatt av BATCHNAME-UniqueId
     */
    String launch(BatchArguments arguments);

    /**
     * Unikt batchnavn etter følgende mønster:
     *     B<appnavn><løpenummer>
     *     Eks: BVLFP001 - Grensesnittavstemning
     *
     * @return unikt batchnavn
     */
    String getBatchName();
}
