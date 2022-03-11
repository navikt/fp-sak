package no.nav.foreldrepenger.behandling.kabal;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.AzureADRestClient;

@ApplicationScoped
public class KabalKlient {

    private static final Logger LOG = LoggerFactory.getLogger(KabalKlient.class);
    private static boolean erDeployment = Environment.current().isProd() || Environment.current().isDev();

    private URI uri;
    private String uriString;
    private AzureADRestClient restClient;

    @Inject
    public KabalKlient(
        @KonfigVerdi(value = "KABAL_API_URL", defaultVerdi = "https://kabal-api.intern.nav.no/api/oversendelse/v3/sak") URI uri,
        @KonfigVerdi(value = "KABAL_API_SCOPES", defaultVerdi = "api://prod-gcp.klage.kabal-api/.default") String scope) {
        this.restClient = AzureADRestClient.builder().scope(scope).build();
        this.uri = uri;
        this.uriString = uri.toString();
    }

    KabalKlient() {
        // CDI
    }

    public void sendTilKabal(TilKabalDto request) {
        if (!erDeployment) return;
        try {
            restClient.post(uri, request);
        } catch (Exception e) {
            LOG.warn("KABAL oversend: feil ved sending til KABAL {}", uriString, e);
            throw new TekniskException( "FP-180127", String.format("KABAL %s gir feil, ta opp med team klage.", uriString), e);
        }
    }

}
