package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.nom;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

@ApplicationScoped
public class SkjermetPersonKlient {

    private static final String DEFAULT_URI = "http://skjermede-personer-pip.nom/skjermet";

    private static final Logger LOG = LoggerFactory.getLogger(SkjermetPersonKlient.class);

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
        if (fnr == null) return false;
        try {
            var request = new SkjermetRequestDto(fnr);
            var skjermet = restClient.post(uri, request);
            if ("true".equalsIgnoreCase(skjermet)) {
                LOG.info("FPSAK skjermet person funnet");
                return true;
            }
        } catch (Exception e) {
            LOG.info("FPSAK skjermet person ga feil", e);
            //throw InfotrygdRestFeil.FACTORY.feilfratjeneste(uriString).toException();
        }
        return false;
    }

    private static record SkjermetRequestDto(String personident) {}

}
