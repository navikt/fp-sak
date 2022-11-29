package no.nav.foreldrepenger.økonomistøtte.simulering.klient;

import no.nav.vedtak.exception.IntegrasjonException;

public class OppdragForventetNedetidException extends IntegrasjonException {

    public static final String MELDING = "Kallet mot oppdragsystemet feilet. Feilmelding og tidspunktet tilsier at oppdragsystemet har forventet nedetid (utenfor åpningstid).";

    public OppdragForventetNedetidException() {
        super("FPO-273196", MELDING);
    }

}
