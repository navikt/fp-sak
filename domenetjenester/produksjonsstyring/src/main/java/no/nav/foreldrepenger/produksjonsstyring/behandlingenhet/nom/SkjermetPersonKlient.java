package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.nom;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

@ApplicationScoped
public class SkjermetPersonKlient {

    private static final String DEFAULT_URI = "http://skjermede-personer-pip.nom/skjermet";

    private static final Logger LOG = LoggerFactory.getLogger(SkjermetPersonKlient.class);

    private static final boolean TESTENV = Environment.current().isLocal() || Environment.current().isVTP();

    private OidcRestClient restClient;
    private URI uri;

    @Inject
    public SkjermetPersonKlient(OidcRestClient restClient, @KonfigVerdi(value = "skjermet.person.rs.url", defaultVerdi = DEFAULT_URI) URI uri) {
        this.restClient = restClient;
        this.uri = uri;
    }

    public SkjermetPersonKlient() {
        // CDI
    }


    public boolean erSkjermet(String fnr) {
        if (TESTENV || fnr == null) return false;

        var request = new SkjermetRequestDto(fnr);
        var skjermet = restClient.post(uri, request);
        return "true".equalsIgnoreCase(skjermet);
    }

    private static record SkjermetRequestDto(String personident) {}

}
