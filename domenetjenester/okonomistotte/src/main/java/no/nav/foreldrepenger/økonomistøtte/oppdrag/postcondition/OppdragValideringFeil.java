package no.nav.foreldrepenger.økonomistøtte.oppdrag.postcondition;

import no.nav.vedtak.exception.TekniskException;

public class OppdragValideringFeil {

    static TekniskException valideringsfeil(String detaljer) {
        return new TekniskException("FP-767898", String.format("Validering av oppdrag feilet: %s", detaljer));
    }

    static TekniskException minorValideringsfeil(String detaljer) {
        return new TekniskException("FP-577348", String.format("Oppdaget mindre forskjell mellom tilkjent ytelse oppdrag: %s", detaljer));
    }
}
