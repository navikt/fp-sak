package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.kontrakter.risk.v1.LagreFaresignalVurderingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.rest.FaresignalerRespons;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class FpriskTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FpriskTjeneste.class);

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
    private LRUCache<String, FaresignalerRespons> faresignalerCache = new LRUCache<>(1000, CACHE_ELEMENT_LIVE_TIME_MS);

    private static final String ENDPOINT_FPRISK = "fprisk.url";

    private URI lagreVurderingEndpoint;
    private URI hentRisikoklassifiseringEndpoint;
    private OidcRestClient oidcRestClient;

    FpriskTjeneste() {
        // CDI
    }

    @Inject
    public FpriskTjeneste(@KonfigVerdi(ENDPOINT_FPRISK) URI fpriskEndpoint,
                          OidcRestClient oidcRestClient) {
        this.oidcRestClient = oidcRestClient;
        this.hentRisikoklassifiseringEndpoint = toUri(fpriskEndpoint, "/api/risikovurdering/hent");
        this.lagreVurderingEndpoint = toUri(fpriskEndpoint, "/api/risikovurdering/lagreVurdering");
    }

    public Optional<FaresignalerRespons> hentFaresignalerForBehandling(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid, "behandlingUuid");

        var uuidString = behandlingUuid.toString();
        if (faresignalerCache.get(uuidString) != null) {
            return Optional.of(faresignalerCache.get(uuidString));
        }

        var request = new FaresignalerRequest(behandlingUuid);
        try {
            var respons = oidcRestClient.post(hentRisikoklassifiseringEndpoint, request, FaresignalerRespons.class);
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
        var request = new LagreFaresignalVurderingDto(behandlingUuid, mapTilFaresignalKontrakt(faresignalVurdering));
        try {
            oidcRestClient.post(lagreVurderingEndpoint, request);
        } catch (Exception e) {
            LOG.warn("Klarte ikke lagre risikovurdering i fprisk", e);
        }
    }

    private no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering mapTilFaresignalKontrakt(FaresignalVurdering faresignalVurdering) {
        return switch (faresignalVurdering) {
            case INNVIRKNING -> no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering.INNVIRKNING;
            case INNVILGET_REDUSERT -> no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering.INNVILGET_REDUSERT;
            case INNVILGET_UENDRET -> no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering.INNVILGET_UENDRET;
            case AVSLAG_FARESIGNAL -> no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering.AVSLAG_FARESIGNAL;
            case AVSLAG_ANNET -> no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering.AVSLAG_ANNET;
            case INGEN_INNVIRKNING -> no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering.INGEN_INNVIRKNING;
            case UDEFINERT -> throw new IllegalStateException("Kode UDEFINERT er ugyldig vurdering av faresignaler");
        };
    }

    private URI toUri(URI endpointURI, String path) {
        try {
            return UriBuilder.fromUri(endpointURI).path(path).build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Ugyldig uri: " + endpointURI + path, e);
        }
    }

    private static record FaresignalerRequest(UUID konsumentId){}
}
