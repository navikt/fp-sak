package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import java.net.HttpURLConnection;

import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.exception.VLLogLevel;

public class BehandlingEndretException extends TekniskException {

    public BehandlingEndretException() {
        super("FP-837578", "Behandlingen er endret av en annen saksbehandler, eller har blitt oppdatert med ny informasjon av systemet. Last inn behandlingen på nytt.");
    }

    @Override
    public int getStatusCode() {
        return HttpURLConnection.HTTP_CONFLICT;
    }

    @Override
    public String getFeilkode() {
        return "BEHANDLING_ENDRET_FEIL";
    }

    @Override
    public VLLogLevel getLogLevel() {
        return VLLogLevel.NOLOG;
    }

}
