package no.nav.foreldrepenger.web.app.soap.sak.v1;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.client.utils.URIBuilder;

import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class FpfordelRestKlient {
    private static final String ENDPOINT_KEY = "fpfordel.base.url";
    private static final String DEFAULT_URI = "http://fpfordel/fpfordel";
    private static final String PATH_UTLED = "/api/vurdering/ytelsetype";

    private OidcRestClient oidcRestClient;
    private URI endpointUtledYtelseType;

    public FpfordelRestKlient() {
    }

    @Inject
    public FpfordelRestKlient(OidcRestClient oidcRestClient,
                              @KonfigVerdi(value = ENDPOINT_KEY, defaultVerdi = DEFAULT_URI) URI endpoint) {
        this.oidcRestClient = oidcRestClient;
        this.endpointUtledYtelseType = URI.create(endpoint.toString() + PATH_UTLED);
    }

    public String utledYtelestypeFor(JournalpostId journalpost) {
        try {
            var request = new URIBuilder(endpointUtledYtelseType)
                .addParameter("journalpostId", journalpost.getVerdi());
            return oidcRestClient.get(request.build(), String.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Noe galt skjedde", e);
        }
    }
}
