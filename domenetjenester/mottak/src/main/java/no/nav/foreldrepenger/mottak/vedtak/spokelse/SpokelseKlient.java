package no.nav.foreldrepenger.mottak.vedtak.spokelse;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.OAuth2RestClient;

@ApplicationScoped
public class SpokelseKlient {

    private static final String AZURE_V2_TOKEN_ENDPOINT_PROD = "https://login.microsoftonline.com/62366534-1ec3-4962-8869-9b5535279d0b/oauth2/v2.0/token";
    private static final String SPOKELSE_GRUNNLAG_DEFAULT_URL = "http://spokelse.tbd/grunnlag";

    private static final Logger LOG = LoggerFactory.getLogger(SpokelseKlient.class);

    private URI uri;
    private String uriString;
    private OAuth2RestClient restClient;

    @Inject
    public SpokelseKlient(
        @KonfigVerdi(value = "SPOKELSE_GRUNNLAG_URL", defaultVerdi = SPOKELSE_GRUNNLAG_DEFAULT_URL) URI uri,
        @KonfigVerdi(value = "SPOKELSE_GRUNNLAG_SCOPES", defaultVerdi = "spokelse/.default") String scopesCsv,
        @KonfigVerdi(value = "AZURE_APP_CLIENT_ID", defaultVerdi = "fp-sak") String clientId,
        @KonfigVerdi(value = "AZURE_APP_CLIENT_SECRET", defaultVerdi = "fp-sak") String clientSecret,
        @KonfigVerdi(value = "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT", defaultVerdi = AZURE_V2_TOKEN_ENDPOINT_PROD) URI tokenEndpoint,
        @KonfigVerdi(value = "AZURE_HTTP_PROXY", required = false) URI httpProxy) {
        this.restClient = OAuth2RestClient.builder()
            .clientId(clientId)
            .clientSecret(clientSecret)
            .scopes(scopesFraCsv(scopesCsv))
            .tokenEndpoint(tokenEndpoint)
            .tokenEndpointProxy(httpProxy)
            .build();
        this.uri = uri;
        this.uriString = uri.toString();
    }

    SpokelseKlient() {
        // CDI
    }

    public List<SykepengeVedtak> hentGrunnlag(String fnr) {
        try {
            var request = new URIBuilder(uri)
                    .addParameter("fodselsnummer", fnr)
                    .build();
            var grunnlag = restClient.get(request, SykepengeVedtak[].class);
            return Arrays.asList(grunnlag);
        } catch (Exception e) {
            LOG.error("fpsak spokelse feil ved oppslag mot {}, returnerer ingen grunnlag", uriString, e);
            throw new TekniskException("FP-180126", String.format("SPokelse %s gir feil, ta opp med team sykepenger.", uriString), e);
        }
    }

    public List<SykepengeVedtak> hentGrunnlagFailSoft(String fnr) {
        try {
            var request = new URIBuilder(uri)
                .addParameter("fodselsnummer", fnr)
                .build();
            var grunnlag = restClient.get(request, SykepengeVedtak[].class);
            return Arrays.asList(grunnlag);
        } catch (Exception e) {
            LOG.info("fpsak spokelse Feil ved oppslag mot {}, returnerer ingen grunnlag", uriString, e);
            return Collections.emptyList();
        }
    }

    private static Set<String> scopesFraCsv(String scopesCsv) {
        return Set.of(scopesCsv.replace(" ", "").split(","));
    }
}
