package no.nav.foreldrepenger.økonomi.ny.postcondition;

import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.feil.Feil;

public class OppdragValideringException extends TekniskException {
    public OppdragValideringException(Feil feil) {
        super(feil);
    }
}
