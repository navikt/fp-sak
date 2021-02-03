package no.nav.foreldrepenger.økonomistøtte.simulering.klient;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.vedtak.felles.integrasjon.rest.SystemUserOidcRestClient;

@ApplicationScoped
public class FpoppdragSystembrukerRestKlient {

    private static final String FPOPPDRAG_KANSELLER_SIMULERING = "/simulering/kanseller";

    private SystemUserOidcRestClient restClient;
    private URI uriKansellerSimulering;

    public FpoppdragSystembrukerRestKlient() {
        //for cdi proxy
    }

    @Inject
    public FpoppdragSystembrukerRestKlient(SystemUserOidcRestClient restClient) {
        this.restClient = restClient;
        String fpoppdragBaseUrl = FpoppdragFelles.getFpoppdragBaseUrl();

        uriKansellerSimulering = URI.create(fpoppdragBaseUrl + FPOPPDRAG_KANSELLER_SIMULERING);
    }

    /**
     * Kansellerer aktivt simuleringresultat for behandling, hvis det finnes.
     *
     * @param behandlingId
     */
    public void kansellerSimulering(Long behandlingId) {
        restClient.post(uriKansellerSimulering, new BehandlingIdDto(behandlingId));
    }

}
