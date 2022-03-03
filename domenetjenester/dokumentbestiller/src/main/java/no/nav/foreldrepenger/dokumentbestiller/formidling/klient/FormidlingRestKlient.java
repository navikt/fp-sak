package no.nav.foreldrepenger.dokumentbestiller.formidling.klient;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.dokumentbestiller.formidling.Brev;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingV2Dto;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

@ApplicationScoped
public class FormidlingRestKlient implements Brev {

    private OidcRestClient restClient;

    FormidlingRestKlient() {
        //for cdi proxy
    }

    @Inject
    public FormidlingRestKlient(OidcRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void bestill(DokumentbestillingV2Dto dokumentbestillingV2Dto) {
        restClient.post(toUri(baseUrl(), "/api/brev/bestill"), dokumentbestillingV2Dto);
    }

    private URI baseUrl() {
        return Environment.current().getRequiredProperty("fpformidling.base.url", URI.class);
    }

    private URI toUri(URI baseURI, String path) {
        try {
            return UriBuilder.fromUri(baseURI).path(path).build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Ugyldig uri: " + baseURI + path, e);
        }
    }
}
