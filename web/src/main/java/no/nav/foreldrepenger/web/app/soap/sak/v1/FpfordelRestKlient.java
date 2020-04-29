package no.nav.foreldrepenger.web.app.soap.sak.v1;

import java.net.URI;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.client.utils.URIBuilder;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class FpfordelRestKlient {
    private static final String ENDPOINT_KEY = "fpfordel.base.url";
    private static final String DEFAULT_URI = "http://fpfordel/fpfordel";
    private static final String PATH_KANOPPRETTE = "/api/vurdering/kanopprettesak";
    private static final String PATH_UTLED = "/api/vurdering/ytelsetype";

    private OidcRestClient oidcRestClient;
    private URI endpointKanOpprette;
    private URI endpointUtledYtelseType;

    public FpfordelRestKlient() {
    }

    @Inject
    public FpfordelRestKlient(OidcRestClient oidcRestClient,
                              @KonfigVerdi(value = ENDPOINT_KEY, defaultVerdi = DEFAULT_URI) URI endpoint) {
        this.oidcRestClient = oidcRestClient;
        this.endpointKanOpprette = URI.create(endpoint.toString() + PATH_KANOPPRETTE);
        this.endpointUtledYtelseType = URI.create(endpoint.toString() + PATH_UTLED);
    }

    public Boolean kanOppretteSakFra(JournalpostId journalpost, BehandlingTema oppgitt, List<BehandlingTema> aktiveSaker) {
        try {
            var request = new URIBuilder(endpointKanOpprette)
                .addParameter("journalpostId", journalpost.getVerdi())
                .addParameter("oppgittBt", oppgitt.getOffisiellKode());
            aktiveSaker.forEach(bt -> request.addParameter("aktivesakerBt", bt.getOffisiellKode()));
            return oidcRestClient.get(request.build(), Boolean.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Noe galt skjedde", e);
        }
    }

    public FagsakYtelseType utledYtelestypeFor(JournalpostId journalpost) {
        try {
            var request = new URIBuilder(endpointUtledYtelseType)
                .addParameter("journalpostId", journalpost.getVerdi());
            return FagsakYtelseType.fraKode(oidcRestClient.get(request.build(), String.class));
        } catch (Exception e) {
            throw new IllegalArgumentException("Noe galt skjedde", e);
        }
    }
}
