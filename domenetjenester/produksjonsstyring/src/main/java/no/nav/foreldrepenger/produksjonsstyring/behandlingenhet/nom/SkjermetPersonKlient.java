package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.nom;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.vedtak.felles.integrasjon.rest.AzureADRestClient;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
public class SkjermetPersonKlient {
    private static final String DEFAULT_URI = "https://skjermede-personer-pip.intern.nav.no/skjermet";
    private static final String DEFAULT_AZURE_SCOPE = "api://prod-gcp.nom.skjermede-personer-pip/.default";


    private static final boolean TESTENV = Environment.current().isLocal() || Environment.current().isVTP();

    private URI uri;
    private AzureADRestClient client;

    @Inject
    public SkjermetPersonKlient(@KonfigVerdi(value = "skjermet.person.rs.url", defaultVerdi = DEFAULT_URI) URI uri,
                                @KonfigVerdi(value = "skjermet.person.rs.azure.scope", defaultVerdi = DEFAULT_AZURE_SCOPE) String scope) {
        this.client = AzureADRestClient.builder().scope(scope).build();
        this.uri = uri;
    }

    public SkjermetPersonKlient() {
        // CDI
    }


    public boolean erSkjermet(String fnr) {
        if (TESTENV || fnr == null) return false;

        var request = new SkjermetRequestDto(fnr);
        var skjermet = client.post(uri, request);
        return "true".equalsIgnoreCase(skjermet);
    }

    private record SkjermetRequestDto(String personident) {}

}
