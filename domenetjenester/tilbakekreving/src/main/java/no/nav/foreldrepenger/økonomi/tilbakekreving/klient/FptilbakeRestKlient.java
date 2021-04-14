package no.nav.foreldrepenger.økonomi.tilbakekreving.klient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.client.utils.URIBuilder;

import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

@ApplicationScoped
public class FptilbakeRestKlient {

    public static final String FPTILBAKE_HENT_ÅPEN_TILBAKEKREVING = "/behandlinger/tilbakekreving/aapen";

    public static final String FPTILBAKE_HENT_TILBAKEKREVING_VEDTAK_INFO = "/behandlinger/tilbakekreving/vedtak-info";

    public static final String FPTILBAKE_HENT_TILBAKEKREVING_BEHANDLING_INFO = "/behandlinger";

    private OidcRestClient restClient;

    public FptilbakeRestKlient() {
        // for CDI proxy
    }

    @Inject
    public FptilbakeRestKlient(OidcRestClient restClient) {
        this.restClient = restClient;
    }

    public boolean harÅpenTilbakekrevingsbehandling(Saksnummer saksnummer) {
        var uriHentÅpenTilbakekreving = lagRequestUri(saksnummer);
        return restClient.get(uriHentÅpenTilbakekreving, Boolean.class);
    }

    public TilbakekrevingVedtakDto hentTilbakekrevingsVedtakInfo(UUID uuid){
        var uriHentTilbakekrevingVedtaksInfo = lagRequestUri(uuid, FPTILBAKE_HENT_TILBAKEKREVING_VEDTAK_INFO);
        return restClient.get(uriHentTilbakekrevingVedtaksInfo, TilbakekrevingVedtakDto.class);
    }

    public TilbakeBehandlingDto hentBehandlingInfo(UUID uuid){
        var uriHentTilbakekrevingVedtaksInfo = lagRequestUri(uuid, FPTILBAKE_HENT_TILBAKEKREVING_BEHANDLING_INFO);
        return restClient.get(uriHentTilbakekrevingVedtaksInfo, TilbakeBehandlingDto.class);
    }

    private URI lagRequestUri(Saksnummer saksnummer) {
        var endpoint = FptilbakeFelles.getFptilbakeBaseUrl() + FPTILBAKE_HENT_ÅPEN_TILBAKEKREVING;
        try {
            return new URIBuilder(endpoint).addParameter("saksnummer", saksnummer.getVerdi()).build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
    private URI lagRequestUri(UUID uuid, String endpoint) {
        var endpointURI = FptilbakeFelles.getFptilbakeBaseUrl() + endpoint;
        try {
            return new URIBuilder(endpointURI).addParameter("uuid", uuid.toString()).build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }


}
