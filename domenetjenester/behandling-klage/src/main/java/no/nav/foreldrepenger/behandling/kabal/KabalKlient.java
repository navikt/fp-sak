package no.nav.foreldrepenger.behandling.kabal;

import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "kabal.api.url",
    endpointDefault = "https://kabal-api.intern.nav.no/api/oversendelse/v4/sak",
    scopesProperty = "kabal.api.scopes", scopesDefault = "api://prod-gcp.klage.kabal-api/.default")
public class KabalKlient {

    private static final Logger LOG = LoggerFactory.getLogger(KabalKlient.class);
    private static final boolean IS_LOCAL = Environment.current().isLocal();

    private final RestClient restClient;
    private final RestConfig restConfig;

    public KabalKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(KabalKlient.class);
    }

    public void sendTilKabal(TilKabalDto tilKabalDto) {
        if (IS_LOCAL) return; // Har ikke laget kabal-mock i VTP
        try {
            var request = RestRequest.newPOSTJson(tilKabalDto, restConfig.endpoint(), restConfig);
            restClient.sendExpectConflict(request, String.class);
        } catch (Exception e) {
            LOG.warn("KABAL oversend: feil ved sending til KABAL {}", restConfig.endpoint(), e);
            throw new TekniskException( "FP-180127", String.format("KABAL %s gir feil, ta opp med team klage.", restConfig.endpoint()), e);
        }
    }
}
