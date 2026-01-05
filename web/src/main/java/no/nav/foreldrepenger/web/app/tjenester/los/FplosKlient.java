package no.nav.foreldrepenger.web.app.tjenester.los;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.hendelser.behandling.los.LosBehandlingDto;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, application = FpApplication.FPLOS)
public class FplosKlient {

    private final URI lagreBehandlingEndpoint;
    private final RestClient restClient;
    private final RestConfig restConfig;

    public FplosKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        this.lagreBehandlingEndpoint = toUri(restConfig.fpContextPath(), "/api/migrering/lagrebehandling");
    }

    /**
     * Sender en behandling til fplos
     */
    void sendBehandlingTilLos(LosBehandlingDto losBehandlingDto) {
        var request = RestRequest.newPOSTJson(losBehandlingDto, lagreBehandlingEndpoint, restConfig);
        restClient.sendReturnOptional(request, String.class);
    }

    private URI toUri(URI endpointURI, String path) {
        try {
            return UriBuilder.fromUri(endpointURI).path(path).build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Ugyldig uri: " + endpointURI + path, e);
        }
    }
}
