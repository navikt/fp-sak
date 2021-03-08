package no.nav.foreldrepenger.økonomistøtte.simulering.tjeneste;

import no.nav.vedtak.exception.TekniskException;

final class SimulerOppdragIntegrasjonTjenesteFeil {

    private SimulerOppdragIntegrasjonTjenesteFeil() {
    }

    static TekniskException startSimuleringFeiletMedFeilmelding(Long behandlingId, Exception e) {
        return new TekniskException("FP-423523", "Start simulering feilet for behandlingId: " + behandlingId, e);
    }
}
