package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.VLLogLevel;

public class ForvaltningException extends FunksjonellException {

    public ForvaltningException(String message) {
        super("FORVALTNING", message);
    }

    @Override
    public String getFeilkode() {
        return "FORVALTNING";
    }

    @Override
    public VLLogLevel getLogLevel() {
        return VLLogLevel.NOLOG;
    }
}
