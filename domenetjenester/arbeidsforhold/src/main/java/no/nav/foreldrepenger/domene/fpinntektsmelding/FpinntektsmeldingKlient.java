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
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "fpinntektsmelding.url", endpointDefault = "https://ftinntektsmelding.intern.dev.nav.no/ftinntektsmelding",
    scopesProperty = "fpinntektsmelding.scopes", scopesDefault = "api://dev-gcp.teamforeldrepenger.ftinntektsmelding/.default", application = FpApplication.FPINNTEKTSMELDING)
public class FpinntektsmeldingKlient {
    private static final String FP_INNTEKSTMELDING_OPPRETT_FORESPORSEL = "/api/foresporsel/opprett";

    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI uriOpprettForesporsel;

    public FpinntektsmeldingKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(FpinntektsmeldingKlient.class);
        this.uriOpprettForesporsel = UriBuilder.fromUri(restConfig.fpContextPath()).path(FP_INNTEKSTMELDING_OPPRETT_FORESPORSEL).build();
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
}

