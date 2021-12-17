package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.kontrakter.risk.v1.HentRisikovurderingDto;
import no.nav.foreldrepenger.kontrakter.risk.v1.LagreFaresignalVurderingDto;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikovurderingResultatDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
public class FpriskTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FpriskTjeneste.class);
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
        this.hentRisikoklassifiseringEndpoint = toUri(fpriskEndpoint, "/api/risikovurdering/hentResultat");
        this.lagreVurderingEndpoint = toUri(fpriskEndpoint, "/api/risikovurdering/lagreVurdering");
    }

    public Optional<RisikovurderingResultatDto> hentFaresignalerForBehandling(HentRisikovurderingDto request) {
        Objects.requireNonNull(request, "request");
        try {
            var respons = oidcRestClient.post(hentRisikoklassifiseringEndpoint, request, RisikovurderingResultatDto.class);
            return Optional.ofNullable(respons);
        } catch (Exception e) {
            LOG.warn("Klarte ikke hente faresignaler fra fprisk", e);
            return Optional.empty();
        }
    }

    public void sendRisikovurderingTilFprisk(LagreFaresignalVurderingDto request) {
        Objects.requireNonNull(request, "behandlingUuid");
        try {
            oidcRestClient.post(lagreVurderingEndpoint, request);
        } catch (Exception e) {
            LOG.warn("Klarte ikke lagre risikovurdering i fprisk", e);
        }
    }

    private URI toUri(URI endpointURI, String path) {
        try {
            return UriBuilder.fromUri(endpointURI).path(path).build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Ugyldig uri: " + endpointURI + path, e);
        }
    }
}
