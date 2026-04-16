package no.nav.foreldrepenger.økonomistøtte.simulering.klient;

import java.net.HttpURLConnection;

import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.VLLogLevel;

public class OppdragForventetNedetidException extends IntegrasjonException {

    public static final String MELDING = "Kallet mot oppdragsystemet feilet. Feilmelding og tidspunktet tilsier at oppdragsystemet har forventet nedetid (utenfor åpningstid).";

    public OppdragForventetNedetidException() {
        super("FP-273196", MELDING);
    }

    @Override
    public int getStatusCode() {
        return HttpURLConnection.HTTP_UNAVAILABLE;
    }

    @Override
    public String getFeilkode() {
        return "OPPDRAG_FORVENTET_NEDETID";
    }

    @Override
    public VLLogLevel getLogLevel() {
        return VLLogLevel.NOLOG;
    }

}
