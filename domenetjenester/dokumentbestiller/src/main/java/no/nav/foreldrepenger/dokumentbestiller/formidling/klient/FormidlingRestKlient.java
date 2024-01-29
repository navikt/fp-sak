package no.nav.foreldrepenger.dokumentbestiller.formidling.klient;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.dokumentbestiller.formidling.Brev;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingDto;
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

    private final static String BESTILL_URL = "/bestill";
    private final static String FORHÅNDSVIS_URL = "/forhaandsvis";
    private final URI uri;

    public FormidlingRestKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        this.uri = UriBuilder.fromUri(restConfig.fpContextPath()).path("/api/brev").build();
    }

    @Override
    public void bestill(DokumentbestillingV2Dto dokumentbestillingV2Dto) {
        var request = RestRequest.newPOSTJson(dokumentbestillingV2Dto, UriBuilder.fromUri(uri).path(BESTILL_URL).build(), restConfig);
        restClient.sendReturnOptional(request, String.class);
    }

    public byte[] forhåndsvis(DokumentbestillingDto dokumentbestillingDto) {
        var request = RestRequest.newPOSTJson(dokumentbestillingDto, UriBuilder.fromUri(uri).path(FORHÅNDSVIS_URL).build(), restConfig);
        return restClient.sendReturnByteArray(request);
    }
}
