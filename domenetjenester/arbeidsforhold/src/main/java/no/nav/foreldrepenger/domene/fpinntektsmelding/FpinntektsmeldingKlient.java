package no.nav.foreldrepenger.domene.fpinntektsmelding;

import java.net.URI;
import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, application = FpApplication.FPINNTEKTSMELDING)
public class FpinntektsmeldingKlient {

    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI uriOpprettForesporsel;

    public FpinntektsmeldingKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(FpinntektsmeldingKlient.class);
        this.uriOpprettForesporsel = toUri(restConfig.fpContextPath(), "/api/foresporsel/opprett");
    }

    public void opprettForespørsel(OpprettForespørselRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            var rrequest = RestRequest.newPOSTJson(request, uriOpprettForesporsel, restConfig);
            restClient.send(rrequest, String.class);
        } catch (Exception e) {
            throw new IllegalStateException("Klarte ikke opprette forespørsel om inntektsmelding", e);
        }
    }

    private URI toUri(URI endpointURI, String path) {
        try {
            return UriBuilder.fromUri(endpointURI).path(path).build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Ugyldig uri: " + endpointURI + path, e);
        }
    }

}

