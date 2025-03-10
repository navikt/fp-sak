package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import jakarta.enterprise.context.Dependent;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.vedtak.felles.integrasjon.rest.*;

import java.net.URI;
import java.util.Set;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, application = FpApplication.FPTILGANG)
public class RutingKlient {

    private final URI rutingUri;
    private final RestClient klient;
    private final RestConfig restConfig;

    public RutingKlient() {
        this.klient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        this.rutingUri = UriBuilder.fromUri(restConfig.fpContextPath())
            .path("/api/ruting/egenskaper")
            .build();
    }

    public Set<RutingResultat> finnRutingEgenskaper(Set<String> aktørIdenter) {
        var request = new RutingRequest(aktørIdenter);
        var rrequest = RestRequest.newPOSTJson(request, rutingUri, restConfig);
        return klient.sendReturnOptional(rrequest, RutingRespons.class).map(RutingRespons::resultater).orElseGet(Set::of);
    }


    public record RutingRequest(@Valid Set<String> aktørIdenter) { }

    public record RutingRespons(Set<RutingResultat> resultater) { }

}
