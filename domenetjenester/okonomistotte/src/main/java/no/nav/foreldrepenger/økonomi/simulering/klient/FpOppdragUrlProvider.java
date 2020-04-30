package no.nav.foreldrepenger.økonomi.simulering.klient;

import no.nav.vedtak.konfig.KonfigVerdi;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class FpOppdragUrlProvider {

    private static final String FPOPPDRAG_DEFAULT_URL = "http://fpoppdrag/fpoppdrag/api";
    private static final String FPOPPDRAG_FRONTEND_PATH = "/fpoppdrag/api";

    private String fpoppdragUrl;

    public FpOppdragUrlProvider() {
        //CDI proxy
    }

    @Inject
    public FpOppdragUrlProvider(@KonfigVerdi(value="fpoppdrag.url", defaultVerdi = FPOPPDRAG_DEFAULT_URL) String fpoppdragUrl) {
        this.fpoppdragUrl = fpoppdragUrl;
    }

    public String getFpoppdragUrl() {
        return fpoppdragUrl;
    }

    public String getFpoppdragFrontendUrl() {
        if ( fpoppdragUrl.equalsIgnoreCase(FPOPPDRAG_DEFAULT_URL) ) {
            return FPOPPDRAG_FRONTEND_PATH;
        }
        return fpoppdragUrl;
    }
}
