package no.nav.foreldrepenger.økonomistøtte.simulering.klient;

import no.nav.foreldrepenger.konfig.Environment;

class FpoppdragFelles {

    private static final Environment ENV = Environment.current();
    static final String FPOPPDRAG_BASE_URL = "http://fpoppdrag/fpoppdrag/api";
    static final String FPOPPDRAG_OVERRIDE_URL = "fpoppdrag.override.direkte.url";

    private FpoppdragFelles() {

    }

    static String getFpoppdragBaseUrl() {
        return ENV.getProperty(FPOPPDRAG_OVERRIDE_URL, FPOPPDRAG_BASE_URL);
    }
}
