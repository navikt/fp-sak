package no.nav.foreldrepenger.økonomistøtte.simulering.klient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.vedtak.felles.integrasjon.rest.*;

import java.net.URI;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, application = FpApplication.FPOPPDRAG)
public class FpoppdragSystembrukerRestKlient {

    private static final String FPOPPDRAG_KANSELLER_SIMULERING = "/api/simulering/kanseller";

    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI uriKansellerSimulering;

    public FpoppdragSystembrukerRestKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        uriKansellerSimulering = UriBuilder.fromUri(restConfig.fpContextPath()).path(FPOPPDRAG_KANSELLER_SIMULERING).build();
    }

    /**
     * Kansellerer aktivt simuleringresultat for behandling, hvis det finnes.
     *
     * @param behandlingId
     */
    public void kansellerSimulering(Long behandlingId) {
        var request = RestRequest.newPOSTJson(new BehandlingIdDto(behandlingId), uriKansellerSimulering, restConfig);
        restClient.sendReturnOptional(request, String.class);
    }

}
