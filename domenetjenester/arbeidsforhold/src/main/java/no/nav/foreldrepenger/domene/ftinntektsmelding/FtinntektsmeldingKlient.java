package no.nav.foreldrepenger.domene.ftinntektsmelding;

import java.util.Objects;

import jakarta.enterprise.context.Dependent;

import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "ftinntektsmelding.url", endpointDefault = "https://ftinntektsmelding.intern.dev.nav.no/ftinntektsmelding",
    scopesProperty = "ftinntektsmelding.scopes", scopesDefault = "api://prod-gcp.teamforeldrepenger.ftinntektsmelding/.default")
public class FtinntektsmeldingKlient {

    private final RestClient restClient;
    private final RestConfig restConfig;

    public FtinntektsmeldingKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(FtinntektsmeldingKlient.class);
    }

    public void opprettForespørsel(OpprettForespørselRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            var rrequest = RestRequest.newPOSTJson(request, restConfig.endpoint(), restConfig);
            restClient.send(rrequest, String.class);
        } catch (Exception e) {
            throw new IllegalStateException("Klarte ikke opprette forespørsel om inntektsmelding", e);
        }
    }
}
