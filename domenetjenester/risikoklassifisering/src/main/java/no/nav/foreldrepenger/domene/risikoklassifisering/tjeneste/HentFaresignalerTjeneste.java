package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.rest.FaresignalerRequest;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.rest.FaresignalerRespons;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class HentFaresignalerTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(HentFaresignalerTjeneste.class);

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
    private LRUCache<String, FaresignalerRespons> faresignalerCache = new LRUCache<>(1000, CACHE_ELEMENT_LIVE_TIME_MS);

    private static final String ENDPOINT_KEY = "fprisk_risikoklassifisering_hent.url";

    private URI endpoint;
    private OidcRestClient oidcRestClient;

    HentFaresignalerTjeneste() {
        // CDI
    }

    @Inject
    public HentFaresignalerTjeneste(@KonfigVerdi(ENDPOINT_KEY) URI endpoint,
                                    OidcRestClient oidcRestClient) {
        this.oidcRestClient = oidcRestClient;
        this.endpoint = endpoint;
    }

    public Optional<FaresignalerRespons> hentFaresignalerForBehandling(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, "behandlingUuid");

        String uuidString = behandlingUuid.toString();
        if (faresignalerCache.get(uuidString) != null) {
            return Optional.of(faresignalerCache.get(uuidString));
        }

        FaresignalerRequest request = new FaresignalerRequest();
        request.setKonsumentId(behandlingUuid);
        try {
            FaresignalerRespons respons = oidcRestClient.post(endpoint, request, FaresignalerRespons.class);
            if (respons != null && respons.getRisikoklasse() != null) {
                faresignalerCache.put(uuidString, respons);
            }
            return Optional.ofNullable(respons);
        } catch (Exception e) {
            LOG.warn("Klarte ikke hente faresignaler fra fprisk", e);
            return Optional.empty();
        }
    }
}
