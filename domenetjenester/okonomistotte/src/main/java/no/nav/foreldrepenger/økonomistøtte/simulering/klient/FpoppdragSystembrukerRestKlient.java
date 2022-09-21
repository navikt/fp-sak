package no.nav.foreldrepenger.økonomistøtte.simulering.klient;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.STS_CC, application = FpApplication.FPOPPDRAG, endpointProperty = "fpoppdrag.override.direkte.url") // Testformål
public class FpoppdragSystembrukerRestKlient {

    private static final String FPOPPDRAG_KANSELLER_SIMULERING = "/api/simulering/kanseller";

    private RestClient restClient;
    private URI uriKansellerSimulering;

    public FpoppdragSystembrukerRestKlient() {
        //for cdi proxy
    }

    @Inject
    public FpoppdragSystembrukerRestKlient(RestClient restClient) {
        this.restClient = restClient;
        var fpoppdragBaseUrl = RestConfig.endpointFromAnnotation(FpoppdragSystembrukerRestKlient.class);
        uriKansellerSimulering = UriBuilder.fromUri(fpoppdragBaseUrl).path(FPOPPDRAG_KANSELLER_SIMULERING).build();
    }

    /**
     * Kansellerer aktivt simuleringresultat for behandling, hvis det finnes.
     *
     * @param behandlingId
     */
    public void kansellerSimulering(Long behandlingId) {
        var request = RestRequest.newPOSTJson(new BehandlingIdDto(behandlingId), uriKansellerSimulering, FpoppdragSystembrukerRestKlient.class);
        restClient.sendReturnOptional(request, String.class);
    }

}
