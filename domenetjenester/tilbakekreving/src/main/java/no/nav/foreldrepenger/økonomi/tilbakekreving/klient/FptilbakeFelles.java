package no.nav.foreldrepenger.økonomi.tilbakekreving.klient;

import no.nav.foreldrepenger.konfig.Environment;

class FptilbakeFelles {

    private static final Environment ENV = Environment.current();

    static final String FPTILBAKE_BASE_URL = "http://fptilbake/fptilbake/api";
    static final String FPTILBAKE_OVERRIDE_URL = "fptilbake.override.direkte.url";

    private FptilbakeFelles() {
        // hindrer instansiering - slik at sonarqube blir glad
    }

    static String getFptilbakeBaseUrl() {
        return ENV.getProperty(FPTILBAKE_OVERRIDE_URL, FPTILBAKE_BASE_URL);
    }
}
