package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.nom;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "skjermet.person.rs.url", endpointDefault = "https://skjermede-personer-pip.intern.nav.no/skjermet",
    scopesProperty = "skjermet.person.rs.azure.scope", scopesDefault = "api://prod-gcp.nom.skjermede-personer-pip/.default")
public class SkjermetPersonKlient {
    private static final Logger LOG = LoggerFactory.getLogger(SkjermetPersonKlient.class);
    private static final boolean TESTENV = Environment.current().isLocal() || Environment.current().isVTP();

    private RestClient client;

    @Inject
    public SkjermetPersonKlient(RestClient restClient) {
        this.client = restClient;
    }

    public SkjermetPersonKlient() {
        // CDI
    }


    public boolean erSkjermet(String fnr) {
        if (TESTENV || fnr == null) return false;

        var request = RestRequest.newPOSTJson(new SkjermetRequestDto(fnr), SkjermetPersonKlient.class)
                    .timeout(Duration.ofSeconds(30));
        try {
            return kallMedSjekk(request);
        } catch (Exception e) {
            LOG.info("SkjermetPerson fikk feil", e);
        }
        return kallMedSjekk(request);
    }

    private boolean kallMedSjekk(RestRequest request) {
        var skjermet = client.send(request, String.class);
        return "true".equalsIgnoreCase(skjermet);
    }

    private record SkjermetRequestDto(String personident) {}

}
