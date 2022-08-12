package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.nom;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.vedtak.felles.integrasjon.rest.AzureADRestClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

@ApplicationScoped
public class SkjermetPersonKlient {

    private static final Logger LOG = LoggerFactory.getLogger(SkjermetPersonKlient.class);
    private static final String DEFAULT_URI = "http://skjermede-personer-pip.nom/skjermet";
    private static final String DEFAULT_URI_GCP = "https://skjermede-personer-pip.intern.nav.no/skjermet";
    private static final String DEFAULT_AZURE_SCOPE = "api://prod-gcp.nom.skjermede-personer-pip/.default";


    private static final boolean TESTENV = Environment.current().isLocal() || Environment.current().isVTP();

    private URI uri;
    private OidcRestClient restClient;
    private URI uriGcp;
    private AzureADRestClient restClientGcp;

    @Inject
    public SkjermetPersonKlient(OidcRestClient restClient,
                                @KonfigVerdi(value = "skjermet.person.rs.url", defaultVerdi = DEFAULT_URI) URI uri,
                                @KonfigVerdi(value = "skjermet.person.rs.url.gcp", defaultVerdi = DEFAULT_URI_GCP) URI uriGcp,
                                @KonfigVerdi(value = "skjermet.person.rs.azure.scope", defaultVerdi = DEFAULT_AZURE_SCOPE) String scope) {
        this.restClient = restClient;
        this.restClientGcp = AzureADRestClient.builder().scope(scope).build();
        this.uri = uri;
        this.uriGcp = uriGcp;
    }

    public SkjermetPersonKlient() {
        // CDI
    }


    public boolean erSkjermet(String fnr) {
        if (TESTENV || fnr == null) return false;

        var request = new SkjermetRequestDto(fnr);
        var skjermet = restClient.post(uri, request);
        sjekkGcp(request, skjermet);
        return "true".equalsIgnoreCase(skjermet);
    }

    private void sjekkGcp(SkjermetRequestDto request, String fssRespons) {
        if (Environment.current().isProd()) return;

        CompletableFuture.supplyAsync(() -> restClientGcp.post(uriGcp, request))
            .thenAccept(gcpRespons -> {
                if (!gcpRespons.equals(fssRespons)) {
                    LOG.warn("SkjermetPersonKlient gir avvik mellom fss og gcp");
                }
            });
    }

    private record SkjermetRequestDto(String personident) {}

}
