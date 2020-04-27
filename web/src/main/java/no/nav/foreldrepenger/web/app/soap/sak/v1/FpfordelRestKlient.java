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
    private static final String ENDPOINT_KEY = "fpfordel.vurder.url";
    private static final String DEFAULT_URI = "http://fpfordel/fpfordel/api/vurdering/kanopprettesak";

    private OidcRestClient oidcRestClient;
    private URI endpoint;

    public FpfordelRestKlient() {
    }

    @Inject
    public FpfordelRestKlient(OidcRestClient oidcRestClient,
                              @KonfigVerdi(value = ENDPOINT_KEY, defaultVerdi = DEFAULT_URI) URI endpoint) {
        this.oidcRestClient = oidcRestClient;
        this.endpoint = endpoint;
    }

    public Boolean kanOppretteSakFra(JournalpostId journalpost) {
        try {
            var request = new URIBuilder(endpoint)
                .addParameter("journalpostId", journalpost.getVerdi())
                .build();
            return oidcRestClient.get(request, Boolean.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Noe galt skjedde", e);
        }
    }
}
