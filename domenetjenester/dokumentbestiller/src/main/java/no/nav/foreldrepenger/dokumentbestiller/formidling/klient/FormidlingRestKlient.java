package no.nav.foreldrepenger.dokumentbestiller.formidling.klient;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.dokumentbestiller.formidling.Dokument;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentBestillingDto;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentForhåndsvisDto;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, application = FpApplication.FPFORMIDLING)
public class FormidlingRestKlient implements Dokument {

    private final RestClient restClient;
    private final RestConfig restConfig;

    private static final String BESTILL_URL = "/bestill";
    private static final String FORHÅNDSVIS_URL = "/forhaandsvis";
    private static final String V3 = "/v3";
    private final URI uri;

    public FormidlingRestKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        this.uri = UriBuilder.fromUri(restConfig.fpContextPath()).path("/api/brev").build();
    }

    @Override
    public void bestill(DokumentBestillingDto dokumentBestillingDto) {
        var request = RestRequest.newPOSTJson(dokumentBestillingDto, UriBuilder.fromUri(uri).path(BESTILL_URL).path(V3).build(), restConfig);
        restClient.sendReturnOptional(request, String.class);
    }

    @Override
    public byte[] forhåndsvis(DokumentForhåndsvisDto dokumentForhåndsvisDto) {
        var request = RestRequest.newPOSTJson(dokumentForhåndsvisDto, UriBuilder.fromUri(uri).path(FORHÅNDSVIS_URL).path(V3).build(), restConfig);
        return restClient.sendReturnByteArray(request);
    }
}
