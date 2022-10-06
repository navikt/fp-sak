package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.nom;

import java.time.Duration;

import javax.enterprise.context.Dependent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.felles.integrasjon.skjerming.AbstractSkjermetPersonKlient;
import no.nav.vedtak.felles.integrasjon.skjerming.Skjerming;

/*
 * Klient for å sjekke om person er skjermet.
 * Grensesnitt se #skjermingsløsningen
 */
@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "skjermet.person.rs.url", endpointDefault = "https://skjermede-personer-pip.intern.nav.no/skjermet",
    scopesProperty = "skjermet.person.rs.azure.scope", scopesDefault = "api://prod-gcp.nom.skjermede-personer-pip/.default")
public class SkjermetPersonKlient implements Skjerming {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractSkjermetPersonKlient.class);
    private static final boolean TESTENV = Environment.current().isLocal();

    private final RestClient client;
    private final RestConfig restConfig;

    protected SkjermetPersonKlient() {
        this.client = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        if (!restConfig.tokenConfig().isAzureAD()) {
            throw new IllegalArgumentException("Utviklerfeil: klient må annoteres med Azure CC");
        }
    }


    @Override
    public boolean erSkjermet(String fnr) {
        if (TESTENV || fnr == null) return false;

        var request = RestRequest.newPOSTJson(new SkjermetRequestDto(fnr), restConfig.endpoint(), restConfig)
            .timeout(Duration.ofSeconds(60));

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
