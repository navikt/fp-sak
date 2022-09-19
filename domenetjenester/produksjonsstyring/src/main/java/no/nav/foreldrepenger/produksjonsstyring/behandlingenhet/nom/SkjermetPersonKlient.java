package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.nom;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "skjermet.person.rs.url", endpointDefault = "https://skjermede-personer-pip.intern.nav.no/skjermet",
    scopesProperty = "skjermet.person.rs.azure.scope", scopesDefault = "api://prod-gcp.nom.skjermede-personer-pip/.default")
public class SkjermetPersonKlient {

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

        var request = RestRequest.newPOSTJson(new SkjermetRequestDto(fnr), SkjermetPersonKlient.class);
        var skjermet = client.send(request, String.class);
        return "true".equalsIgnoreCase(skjermet);
    }

    private record SkjermetRequestDto(String personident) {}

}
