package no.nav.foreldrepenger.domene.person.krr;

import java.net.URI;
import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.felles.integrasjon.rest.NavHeaders;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "krr.rs.uri", endpointDefault = "https://digdir-krr-proxy.intern.nav.no/rest/v1/person",
    scopesProperty = "krr.rs.scopes", scopesDefault = "api://prod-gcp.team-rocket.digdir-krr-proxy/.default")
public class KrrSpråkKlient {

    private static final Logger LOG = LoggerFactory.getLogger(KrrSpråkKlient.class);
    private final URI endpoint;
    private final RestClient restClient;
    private final RestConfig restConfig;

    public KrrSpråkKlient() {
        this(RestClient.client());
    }

    public KrrSpråkKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(KrrSpråkKlient.class);
        this.endpoint = UriBuilder.fromUri(restConfig.endpoint())
            .queryParam("inkluderSikkerDigitalPost", "false")
            .build();
    }

    public Språkkode finnSpråkkodeForBruker(String fnr) {
        try {
            var request = RestRequest.newGET(endpoint, restConfig)
                .header(NavHeaders.HEADER_NAV_PERSONIDENT, fnr)
                .otherCallId(NavHeaders.HEADER_NAV_CALL_ID)
                .timeout(Duration.ofSeconds(3)); // Kall langt avgårde - blokkerer ofte til 3*timeout. Request inn til fpsak har timeout 20s.
            var respons = restClient.sendReturnOptional(request, KrrRespons.class);
            return respons
                .map(KrrRespons::språk)
                .map(Språkkode::defaultNorsk)
                .orElse(Språkkode.NB);
        } catch (ManglerTilgangException manglerTilgangException) {
            LOG.info("KrrSpråkKlient: Mangler tilgang, returnerer default.");
            return Språkkode.NB;
        } catch (IntegrasjonException e) {
            var NOT_FOUND = String.valueOf(Response.Status.NOT_FOUND.getStatusCode());
            var ie = e.getMessage();
            if (ie.contains(NOT_FOUND)) {
                LOG.info("KrrSpråkKlient: fant ikke bruker, returnerer default. Feilmelding: {}", ie);
                return Språkkode.NB;
            }
            throw e;
        } catch (UriBuilderException|IllegalArgumentException e) {
            throw new IllegalArgumentException("Utviklerfeil syntax-exception for KrrSpråkKlient.finnSpråkkodeForBruker");
        }
    }

    record KrrRespons(@JsonProperty("spraak") String språk) { }

}
