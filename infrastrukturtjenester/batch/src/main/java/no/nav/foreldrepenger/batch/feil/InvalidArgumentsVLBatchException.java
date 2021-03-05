package no.nav.foreldrepenger.batch.feil;

public class InvalidArgumentsVLBatchException extends VLBatchException {

    public InvalidArgumentsVLBatchException(String kode, String msg) {
        super(kode, msg);
    }
}
