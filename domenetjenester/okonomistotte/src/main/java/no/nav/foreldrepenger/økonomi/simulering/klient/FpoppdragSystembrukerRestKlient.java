package no.nav.foreldrepenger.økonomi.simulering.klient;

import no.nav.vedtak.felles.integrasjon.rest.SystemUserOidcRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.URI;

@ApplicationScoped
public class FpoppdragSystembrukerRestKlient {

    private static final String FPOPPDRAG_KANSELLER_SIMULERING = "/simulering/kanseller";

    private SystemUserOidcRestClient restClient;
    private FpOppdragUrlProvider fpOppdragUrlProvider;

    public FpoppdragSystembrukerRestKlient() {
        //for cdi proxy
    }

    @Inject
    public FpoppdragSystembrukerRestKlient(SystemUserOidcRestClient restClient, FpOppdragUrlProvider fpOppdragUrlProvider) {
        this.restClient = restClient;
        this.fpOppdragUrlProvider = fpOppdragUrlProvider;
    }

    /**
     * Kansellerer aktivt simuleringresultat for behandling, hvis det finnes.
     *
     * @param behandlingId
     */
    public void kansellerSimulering(Long behandlingId) {
        String fpoppdragBaseUrl = fpOppdragUrlProvider.getFpoppdragUrl();
        restClient.post(URI.create(fpoppdragBaseUrl + FPOPPDRAG_KANSELLER_SIMULERING), new BehandlingIdDto(behandlingId));
    }

}
