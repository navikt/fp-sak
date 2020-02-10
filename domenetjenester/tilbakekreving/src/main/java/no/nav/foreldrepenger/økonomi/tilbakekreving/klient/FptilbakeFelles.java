package no.nav.foreldrepenger.økonomi.tilbakekreving.klient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.konfig.PropertyUtil;

class FptilbakeFelles {

    private static final Logger logger = LoggerFactory.getLogger(FptilbakeFelles.class);

    static final String FPTILBAKE_BASE_URL = "http://fptilbake/fptilbake/api";
    static final String FPTILBAKE_OVERRIDE_URL = "fptilbake.override.direkte.url";

    private FptilbakeFelles() {
        //hindrer instansiering - slik at sonarqube blir glad
    }

    static String getFptilbakeBaseUrl() {
        String overrideUrl = PropertyUtil.getProperty(FptilbakeFelles.FPTILBAKE_OVERRIDE_URL);
        if (overrideUrl != null && !overrideUrl.isEmpty()) {
            logger.info("Overstyrte URL til fptilbake til {}", overrideUrl);
            return overrideUrl;
        } else {
            return FptilbakeFelles.FPTILBAKE_BASE_URL;
        }

    }
}
