package no.nav.foreldrepenger.domene.person.krr;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.apache.http.Header;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.person.krr.KrrScopedAzureAdClientProducer.KrrScoped;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.felles.integrasjon.rest.AzureADRestClient;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class KrrSpråkKlient {

    private static final Logger LOG = LoggerFactory.getLogger(KrrSpråkKlient.class);
    private final static String HEADER_NAV_PERSONIDENT = "Nav-Personident";
    private final static String HEADER_NAV_CALLID = "Nav-Call-Id";
    private URI endpoint;
    private AzureADRestClient restClient;

    public KrrSpråkKlient() {
    }

    @Inject
    public KrrSpråkKlient(@KonfigVerdi(value = "krr.rs.uri") URI endpoint,
                          @KrrScoped AzureADRestClient azureAdKlient) {
        this.endpoint = endpoint;
        this.restClient = azureAdKlient;
    }

    public Språkkode finnSpråkkodeForBruker(String fnr) {
        try {
            var request = new URIBuilder(endpoint)
                .addParameter("inkluderSikkerDigitalPost", "false")
                .build();
            var respons = restClient.get(request, headere(fnr), KrrRespons.class);
            return Optional.ofNullable(respons)
                .map(KrrRespons::språk)
                .map(Språkkode::defaultNorsk)
                .orElse(Språkkode.NB);
        } catch (ManglerTilgangException manglerTilgangException) {
            LOG.info("KrrSpråkKlient: Mangler tilgang, returnerer default.");
            return Språkkode.NB;
        } catch (IntegrasjonException e) {
            final var NOT_FOUND = String.valueOf(Response.Status.NOT_FOUND.getStatusCode());
            var ie = e.getMessage();
            if (ie.contains(NOT_FOUND)) {
                LOG.info("KrrSpråkKlient: fant ikke bruker, returnerer default. Feilmelding: {}", ie);
                return Språkkode.NB;
            }
            throw e;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Utviklerfeil syntax-exception for KrrSpråkKlient.finnSpråkkodeForBruker");
        }
    }

    private Set<Header> headere(String fnr) {
        var personIdentHeader = new BasicHeader(HEADER_NAV_PERSONIDENT, fnr);
        var callId = Optional.ofNullable(MDCOperations.getCallId())
            .orElseGet(MDCOperations::generateCallId);
        var callIdHeader = new BasicHeader(HEADER_NAV_CALLID, callId);
        return Set.of(personIdentHeader, callIdHeader);
    }

    record KrrRespons(@JsonProperty("spraak") String språk) { }

}
