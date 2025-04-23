package no.nav.foreldrepenger.produksjonsstyring.tilbakekreving;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, application = FpApplication.FPTILBAKE)
public class FptilbakeRestKlient {

    public static final String FPTILBAKE_HENT_ÅPEN_TILBAKEKREVING = "/api/behandlinger/tilbakekreving/aapen";

    public static final String FPTILBAKE_HENT_ÅPEN_BEHANDLING = "/api/behandlinger/tilbakekreving/aapen-behandling";

    public static final String FPTILBAKE_HENT_TILBAKEKREVING_VEDTAK_INFO = "/api/behandlinger/tilbakekreving/vedtak-info";

    public static final String FPTILBAKE_HENT_TILBAKEKREVING_BEHANDLING_INFO = "/api/behandlinger";

    private final RestClient restClient;
    private final RestConfig restConfig;

    public FptilbakeRestKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public boolean harÅpenTilbakekrevingsbehandling(Saksnummer saksnummer) {
        var uriHentÅpenTilbakekreving = lagRequestUri(saksnummer, FPTILBAKE_HENT_ÅPEN_TILBAKEKREVING);
        return restClient.send(uriHentÅpenTilbakekreving, Boolean.class);
    }

    public boolean harÅpenBehandling(Saksnummer saksnummer) {
        var uriHentÅpenTilbakekreving = lagRequestUri(saksnummer, FPTILBAKE_HENT_ÅPEN_BEHANDLING);
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

    private RestRequest lagRequestUri(Saksnummer saksnummer, String path) {
        var target =  UriBuilder.fromUri(restConfig.fpContextPath()).path(path)
            .queryParam("saksnummer", saksnummer.getVerdi()).build();
        return RestRequest.newGET(target, restConfig);
    }

    private RestRequest lagRequestUri(UUID uuid, String endpoint) {
        var target = UriBuilder.fromUri(restConfig.fpContextPath()).path(endpoint)
            .queryParam("uuid", uuid.toString()).build();
        return RestRequest.newGET(target, restConfig);
    }


}
