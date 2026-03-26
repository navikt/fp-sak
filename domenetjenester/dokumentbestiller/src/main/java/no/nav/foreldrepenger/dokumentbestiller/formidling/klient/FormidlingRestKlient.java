package no.nav.foreldrepenger.dokumentbestiller.formidling.klient;

import java.net.URI;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentBestillingDto;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentBestillingHtmlDto;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentForhåndsvisDto;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;

public abstract class FormidlingRestKlient {

    private static final String BESTILL_URL = "/bestill";
    private static final String FORHÅNDSVIS_URL = "/forhaandsvis";
    private static final String GENERER_HTML_URL = "/generer/html";
    private static final String V3 = "/v3";

    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI uri;

    protected FormidlingRestKlient(RestClient restClient, RestConfig restConfig) {
        this.restClient = restClient;
        this.restConfig = restConfig;
        this.uri = UriBuilder.fromUri(restConfig.fpContextPath()).path("/api/brev").build();
    }

    public void bestill(DokumentBestillingDto dokumentBestillingDto) {
        var request = RestRequest.newPOSTJson(dokumentBestillingDto, UriBuilder.fromUri(uri).path(BESTILL_URL).path(V3).build(), restConfig);
        restClient.sendReturnOptional(request, String.class);
    }

    public byte[] forhåndsvis(DokumentForhåndsvisDto dokumentForhåndsvisDto) {
        var request = RestRequest.newPOSTJson(dokumentForhåndsvisDto, UriBuilder.fromUri(uri).path(FORHÅNDSVIS_URL).path(V3).build(), restConfig);
        return restClient.sendReturnByteArray(request);
    }

    public String genererHtml(DokumentBestillingHtmlDto dokumentForhåndsvisDto) {
        var request = RestRequest.newPOSTJson(dokumentForhåndsvisDto, UriBuilder.fromUri(uri).path(GENERER_HTML_URL).path(V3).build(), restConfig);
        request.setAndReplaceHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        return restClient.sendReturnResponseString(request).body();
    }
}
