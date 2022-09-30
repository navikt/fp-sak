package no.nav.foreldrepenger.behandling.kabal;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC)
public class KabalKlient {

    private static final Logger LOG = LoggerFactory.getLogger(KabalKlient.class);
    private static boolean erDeployment = Environment.current().isProd() || Environment.current().isDev();

    private URI uri;
    private String scopes;
    private String uriString;
    private RestClient restClient;

    @Inject
    public KabalKlient(RestClient restClient,
                       @KonfigVerdi(value = "kabal.api.url", defaultVerdi = "https://kabal-api.intern.nav.no/api/oversendelse/v3/sak") URI uri,
                       @KonfigVerdi(value = "kabal.api.scopes", defaultVerdi = "api://prod-gcp.klage.kabal-api/.default") String scope) {
        this.restClient = restClient;
        this.uri = uri;
        this.scopes = scope;
        this.uriString = uri.toString();
    }

    KabalKlient() {
        // CDI
    }

    public void sendTilKabal(TilKabalDto request) {
        if (!erDeployment) return;
        try {
            var rrequest = RestRequest.newPOSTJson(request, uri, TokenFlow.AZUREAD_CC, scopes);
            restClient.sendExpectConflict(rrequest, String.class);
        } catch (Exception e) {
            LOG.warn("KABAL oversend: feil ved sending til KABAL {}", uriString, e);
            throw new TekniskException( "FP-180127", String.format("KABAL %s gir feil, ta opp med team klage.", uriString), e);
        }
    }
}
