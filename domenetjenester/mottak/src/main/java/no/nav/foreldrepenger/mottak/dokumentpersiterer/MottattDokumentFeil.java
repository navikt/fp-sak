package no.nav.foreldrepenger.mottak.dokumentpersiterer;

import no.nav.vedtak.exception.TekniskException;

public final class MottattDokumentFeil {

    private MottattDokumentFeil() {
    }

    public static TekniskException ukjentSkjemaType(String skjemaType) {
        throw new TekniskException("FP-947147", "Ukjent dokument " + skjemaType);
    }

}
