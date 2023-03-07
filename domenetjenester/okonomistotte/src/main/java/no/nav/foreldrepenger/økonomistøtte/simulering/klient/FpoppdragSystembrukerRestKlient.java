package no.nav.foreldrepenger.økonomistøtte.simulering.klient;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.UriBuilder;

import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

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
