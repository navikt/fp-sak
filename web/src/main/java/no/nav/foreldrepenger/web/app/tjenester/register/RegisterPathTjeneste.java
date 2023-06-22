package no.nav.foreldrepenger.web.app.tjenester.register;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.integrasjon.rest.NavHeaders;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.NO_AUTH_NEEDED, endpointProperty = "arbeid.og.inntekt.base.url", endpointDefault = "https://arbeid-og-inntekt.nais.adeo.no")
public class RegisterPathTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterPathTjeneste.class);

    private static final String AAREG_PATH = "/api/v2/redirect/sok/arbeidstaker";
    private static final String AINNTEKT_PATH = "/api/v2/redirect/sok/a-inntekt";

    private static final String HEADER_INNTEKT_FILTER = "Nav-A-inntekt-Filter";
    private static final String HEADER_ENHET_FILTER = "Nav-Enhet";
    private static final String HEADER_SAKSNUMMER_FILTER = "Nav-FagsakId";

    private static final String NASJONAL_ENHET = BehandlendeEnhetTjeneste.getNasjonalEnhet().enhetId();

    private final RestClient restClient;
    private final RestConfig restConfig;



    public RegisterPathTjeneste() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(RegisterPathTjeneste.class);
    }

    public String hentAaregPath(PersonIdent ident) {
        var uri = UriBuilder.fromUri(restConfig.endpoint()).path(AAREG_PATH).build();
        var request = RestRequest.newGET(uri, restConfig)
            .header(NavHeaders.HEADER_NAV_PERSONIDENT, ident.getIdent());
        var path = restClient.send(request, String.class);
        LOG.info("ARBEIDOGINNTEKT aareg respons {}", path);
        return path;
    }

    public String hentAinntektPath(PersonIdent ident, String saksnummer, String filter) {
        var uri = UriBuilder.fromUri(restConfig.endpoint()).path(AINNTEKT_PATH).build();
        var request = RestRequest.newGET(uri, restConfig)
            .header(NavHeaders.HEADER_NAV_PERSONIDENT, ident.getIdent())
            .header(HEADER_INNTEKT_FILTER, filter)
            .header(HEADER_SAKSNUMMER_FILTER, saksnummer)
            .header(HEADER_ENHET_FILTER, NASJONAL_ENHET);
        var path = restClient.send(request, String.class);
        LOG.info("ARBEIDOGINNTEKT ainntekt respons {}", path);
        return path;
    }

}
