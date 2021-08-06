package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import no.nav.vedtak.exception.FunksjonellException;

public class ForvaltningException extends FunksjonellException {

    public ForvaltningException(String message) {
        super("FORVALTNING", message);
    }
}
