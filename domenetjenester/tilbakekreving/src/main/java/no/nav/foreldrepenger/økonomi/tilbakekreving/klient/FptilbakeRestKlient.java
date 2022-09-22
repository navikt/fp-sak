package no.nav.foreldrepenger.økonomi.tilbakekreving.klient;

import java.net.URI;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, application = FpApplication.FPTILBAKE, endpointProperty = "FPTILBAKE_OVERRIDE_URL")  // Testformål
public class FptilbakeRestKlient {

    public static final String FPTILBAKE_HENT_ÅPEN_TILBAKEKREVING = "/api/behandlinger/tilbakekreving/aapen";

    public static final String FPTILBAKE_HENT_TILBAKEKREVING_VEDTAK_INFO = "/api/behandlinger/tilbakekreving/vedtak-info";

    public static final String FPTILBAKE_HENT_TILBAKEKREVING_BEHANDLING_INFO = "/api/behandlinger";

    private RestClient restClient;
    private URI uri;

    public FptilbakeRestKlient() {
        // for CDI proxy
    }

    @Inject
    public FptilbakeRestKlient(RestClient restClient) {
        this.restClient = restClient;
        this.uri = RestConfig.contextPathFromAnnotation(FptilbakeRestKlient.class);
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
        var target =  UriBuilder.fromUri(uri).path(FPTILBAKE_HENT_ÅPEN_TILBAKEKREVING)
            .queryParam("saksnummer", saksnummer.getVerdi()).build();
        return RestRequest.newGET(target, FptilbakeRestKlient.class);
    }
    private RestRequest lagRequestUri(UUID uuid, String endpoint) {
        var target = UriBuilder.fromUri(uri).path(endpoint)
            .queryParam("uuid", uuid.toString()).build();
        return RestRequest.newGET(target, FptilbakeRestKlient.class);
    }


}
