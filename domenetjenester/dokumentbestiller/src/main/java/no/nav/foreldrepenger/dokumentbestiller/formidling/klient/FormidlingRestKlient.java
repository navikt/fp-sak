package no.nav.foreldrepenger.dokumentbestiller.formidling.klient;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.dokumentbestiller.formidling.Brev;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingV2Dto;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, application = FpApplication.FPFORMIDLING)
public class FormidlingRestKlient implements Brev {

    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI uri;

    public FormidlingRestKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        this.uri = UriBuilder.fromUri(restConfig.fpContextPath()).path("/api/brev/bestill").build();
    }

    @Override
    public void bestill(DokumentbestillingV2Dto dokumentbestillingV2Dto) {
        var request = RestRequest.newPOSTJson(dokumentbestillingV2Dto, uri, restConfig);
        restClient.sendReturnOptional(request, String.class);
    }
}
