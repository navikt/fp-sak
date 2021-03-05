package no.nav.foreldrepenger.batch.feil;

import no.nav.foreldrepenger.batch.BatchArguments;

public final class BatchFeil {

    private BatchFeil() {
    }

    public static InvalidArgumentsVLBatchException ugyldigeJobParametere(BatchArguments arguments) {
        return new InvalidArgumentsVLBatchException("FP-189013", "Ugyldig job argumenter " + arguments);
    }
}
