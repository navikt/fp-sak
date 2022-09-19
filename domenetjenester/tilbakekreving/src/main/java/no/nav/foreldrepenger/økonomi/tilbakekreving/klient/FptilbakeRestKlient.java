package no.nav.foreldrepenger.økonomi.tilbakekreving.klient;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.CONTEXT)
public class FptilbakeRestKlient {

    public static final String FPTILBAKE_HENT_ÅPEN_TILBAKEKREVING = "/behandlinger/tilbakekreving/aapen";

    public static final String FPTILBAKE_HENT_TILBAKEKREVING_VEDTAK_INFO = "/behandlinger/tilbakekreving/vedtak-info";

    public static final String FPTILBAKE_HENT_TILBAKEKREVING_BEHANDLING_INFO = "/behandlinger";

    private RestClient restClient;

    public FptilbakeRestKlient() {
        // for CDI proxy
    }

    @Inject
    public FptilbakeRestKlient(RestClient restClient) {
        this.restClient = restClient;
    }

    public boolean harÅpenTilbakekrevingsbehandling(Saksnummer saksnummer) {
        var uriHentÅpenTilbakekreving = lagRequestUri(saksnummer);
        return restClient.send(uriHentÅpenTilbakekreving, Boolean.class);
    }

    public TilbakekrevingVedtakDto hentTilbakekrevingsVedtakInfo(UUID uuid){
        var uriHentTilbakekrevingVedtaksInfo = lagRequestUri(uuid, FPTILBAKE_HENT_TILBAKEKREVING_VEDTAK_INFO);
        return restClient.send(uriHentTilbakekrevingVedtaksInfo, TilbakekrevingVedtakDto.class);
    }

    public TilbakeBehandlingDto hentBehandlingInfo(UUID uuid){
        var uriHentTilbakekrevingVedtaksInfo = lagRequestUri(uuid, FPTILBAKE_HENT_TILBAKEKREVING_BEHANDLING_INFO);
        return restClient.send(uriHentTilbakekrevingVedtaksInfo, TilbakeBehandlingDto.class);
    }

    private RestRequest lagRequestUri(Saksnummer saksnummer) {
        var endpoint = FptilbakeFelles.getFptilbakeBaseUrl() + FPTILBAKE_HENT_ÅPEN_TILBAKEKREVING;
        var uri =  UriBuilder.fromUri(endpoint).queryParam("saksnummer", saksnummer.getVerdi()).build();
        return RestRequest.newGET(uri, FptilbakeRestKlient.class);
    }
    private RestRequest lagRequestUri(UUID uuid, String endpoint) {
        var endpointURI = FptilbakeFelles.getFptilbakeBaseUrl() + endpoint;

        var uri = UriBuilder.fromUri(endpointURI).queryParam("uuid", uuid.toString()).build();
        return RestRequest.newGET(uri, FptilbakeRestKlient.class);
    }


}
