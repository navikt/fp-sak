package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import no.nav.vedtak.exception.TekniskException;

public class BehandlingEndretException extends TekniskException {

    public BehandlingEndretException() {
        super("FP-837578", "Behandling er endret i systemet");
    }
}
