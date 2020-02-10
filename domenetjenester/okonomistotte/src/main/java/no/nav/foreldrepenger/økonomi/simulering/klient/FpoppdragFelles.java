package no.nav.foreldrepenger.økonomi.simulering.klient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.konfig.PropertyUtil;

class FpoppdragFelles {

    private static final Logger logger = LoggerFactory.getLogger(FpoppdragFelles.class);

    static final String FPOPPDRAG_BASE_URL = "http://fpoppdrag/fpoppdrag/api";
    static final String FPOPPDRAG_OVERRIDE_URL = "fpoppdrag.override.direkte.url";

    private FpoppdragFelles() {
        //hindrer instansiering - slik at sonarqube blir glad
    }

    static String getFpoppdragBaseUrl() {
        String overrideUrl = PropertyUtil.getProperty(FpoppdragFelles.FPOPPDRAG_OVERRIDE_URL);
        if (overrideUrl != null && !overrideUrl.isEmpty()) {
            logger.info("Overstyrte URL til fpoppdrag til {}", overrideUrl);
            return overrideUrl;
        } else {
            return FpoppdragFelles.FPOPPDRAG_BASE_URL;
        }

    }
}
