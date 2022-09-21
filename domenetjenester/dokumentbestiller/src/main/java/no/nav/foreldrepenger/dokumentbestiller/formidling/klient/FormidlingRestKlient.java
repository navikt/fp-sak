package no.nav.foreldrepenger.dokumentbestiller.formidling.klient;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.dokumentbestiller.formidling.Brev;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingV2Dto;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.CONTEXT, application = FpApplication.FPFORMIDLING, endpointProperty = "FPFORMIDLING_OVERRIDE_URL")
public class FormidlingRestKlient implements Brev {

    private RestClient restClient;
    private URI uri;

    FormidlingRestKlient() {
        //for cdi proxy
    }

    @Inject
    public FormidlingRestKlient(RestClient restClient) {
        this.restClient = restClient;
        var contextPath = RestConfig.contextPathFromAnnotation(FormidlingRestKlient.class);
        this.uri = UriBuilder.fromUri(contextPath).path("/api/brev/bestill").build();
    }

    @Override
    public void bestill(DokumentbestillingV2Dto dokumentbestillingV2Dto) {
        var request = RestRequest.newPOSTJson(dokumentbestillingV2Dto, uri, FormidlingRestKlient.class);
        restClient.sendReturnOptional(request, String.class);
    }

}
