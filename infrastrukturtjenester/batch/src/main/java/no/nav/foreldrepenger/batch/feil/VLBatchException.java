package no.nav.foreldrepenger.batch.feil;

import no.nav.vedtak.exception.TekniskException;

public class VLBatchException extends TekniskException {

    public VLBatchException(String kode, String msg) {
        super(kode, msg);
    }
}
