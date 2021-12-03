package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.rest.LagreVurderingRequest;
import no.nav.foreldrepenger.kontrakter.risk.kodeverk.RisikoklasseType;
import no.nav.foreldrepenger.kontrakter.risk.v1.LagreRisikovurderingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.rest.FaresignalerRequest;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.rest.FaresignalerRespons;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class FpriskTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FpriskTjeneste.class);

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
    private LRUCache<String, FaresignalerRespons> faresignalerCache = new LRUCache<>(1000, CACHE_ELEMENT_LIVE_TIME_MS);

    private static final String ENDPOINT_KEY_HENT = "fprisk.risikoklassifisering.hent.url";
    private static final String ENDPOINT_KEY_VURDER = "fprisk.risikoklassifisering.vurder.url";

    private URI hentEndpoint;
    private URI lagreVurderingEndpoint;
    private OidcRestClient oidcRestClient;

    FpriskTjeneste() {
        // CDI
    }

    @Inject
    public FpriskTjeneste(@KonfigVerdi(ENDPOINT_KEY_HENT) URI hentEndpoint,
                          @KonfigVerdi(ENDPOINT_KEY_VURDER) URI lagreVurderingEndpoint,
                          OidcRestClient oidcRestClient) {
        this.oidcRestClient = oidcRestClient;
        this.hentEndpoint = hentEndpoint;
        this.lagreVurderingEndpoint = lagreVurderingEndpoint;
    }

    public Optional<FaresignalerRespons> hentFaresignalerForBehandling(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, "behandlingUuid");

        var uuidString = behandlingUuid.toString();
        if (faresignalerCache.get(uuidString) != null) {
            return Optional.of(faresignalerCache.get(uuidString));
        }

        var request = new FaresignalerRequest();
        request.setKonsumentId(behandlingUuid);
        try {
            var respons = oidcRestClient.post(hentEndpoint, request, FaresignalerRespons.class);
            if (respons != null && respons.getRisikoklasse() != null) {
                faresignalerCache.put(uuidString, respons);
            }
            return Optional.ofNullable(respons);
        } catch (Exception e) {
            LOG.warn("Klarte ikke hente faresignaler fra fprisk", e);
            return Optional.empty();
        }
    }

    public void sendRisikovurderingTilFprisk(UUID behandlingUuid, FaresignalVurdering faresignalVurdering) {
        Objects.requireNonNull(behandlingUuid, "behandlingUuid");
        var request = new LagreRisikovurderingDto(behandlingUuid, no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering.AVSLAG_FARESIGNAL);
        try {
            oidcRestClient.post(lagreVurderingEndpoint, request);
        } catch (Exception e) {
            LOG.warn("Klarte ikke lagre risikovurdering i fprisk", e);
        }
    }

}
