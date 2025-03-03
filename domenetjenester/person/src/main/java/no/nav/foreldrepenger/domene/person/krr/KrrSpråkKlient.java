package no.nav.foreldrepenger.domene.person.krr;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
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
            var request = RestRequest.newPOSTJson(new Personidenter(List.of(fnr)), endpoint, restConfig)
                .header(NavHeaders.HEADER_NAV_PERSONIDENT, fnr)
                .otherCallId(NavHeaders.HEADER_NAV_CALL_ID)
                .timeout(Duration.ofSeconds(3)); // Kall langt avgårde - blokkerer ofte til 3*timeout. Request inn til fpsak har timeout 20s.
            var respons = restClient.send(request, Kontaktinformasjoner.class);
            if (respons.feil() != null && !respons.feil().isEmpty()) {
                var feilkode = respons.feil().get(fnr);
                if (Kontaktinformasjoner.FeilKode.person_ikke_funnet.equals(feilkode)) {
                    LOG.info("KrrSpråkKlient: fant ikke bruker, returnerer default");
                } else {
                    LOG.warn("KrrSpråkKlient: Uventet feil ved kall til KRR, returnerer default.");
                }
                return Språkkode.NB;
            }
            var person = respons.personer().get(fnr);
            if (!person.aktiv()) {
                LOG.info("KrrSpråkKlient: bruker er inaktiv, returnerer default");
                return Språkkode.NB;
            }
            return Språkkode.defaultNorsk(person.spraak());
        } catch (UriBuilderException|IllegalArgumentException e) {
            throw new IllegalArgumentException("Utviklerfeil syntax-exception for KrrSpråkKlient.finnSpråkkodeForBruker");
        }
    }

    record KrrRespons(@JsonProperty("spraak") String språk) { }

    record Personidenter(List<String> personidenter) {
    }
}
