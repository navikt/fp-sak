package no.nav.foreldrepenger.tilganger.azure;

import java.net.URI;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.tilganger.InnloggetNavAnsattDto;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, application = FpApplication.FPTILGANG)
public class TilgangKlient {

    private final RestClient restClient;
    private final RestConfig restConfig;

    private static final String V2 = "/v2";
    private final URI uri;

    public TilgangKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        this.uri = UriBuilder.fromUri(restConfig.fpContextPath()).path("/api/bruker/informasjon").build();
    }

    public InnloggetNavAnsattDto brukerInfo(String ident) {
        var request = RestRequest.newGET(UriBuilder.fromUri(uri).queryParam("ident", ident).build(), restConfig);
        return restClient.send(request, InnloggetNavAnsattDto.class);
    }

    public InnloggetNavAnsattDto brukerInfoV2(UUID oid) {
        var request = RestRequest.newGET(UriBuilder.fromUri(uri).path(V2).queryParam("oid", oid).build(), restConfig);
        return restClient.send(request, InnloggetNavAnsattDto.class);
    }
}
