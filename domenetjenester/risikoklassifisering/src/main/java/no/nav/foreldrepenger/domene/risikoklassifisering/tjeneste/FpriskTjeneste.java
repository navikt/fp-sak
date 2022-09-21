package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.kontrakter.risk.v1.HentRisikovurderingDto;
import no.nav.foreldrepenger.kontrakter.risk.v1.LagreFaresignalVurderingDto;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikovurderingRequestDto;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikovurderingResultatDto;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.CONTEXT, application = FpApplication.FPRISK, endpointProperty = "FPRISK_OVERRIDE_URL")
public class FpriskTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FpriskTjeneste.class);

    private URI sendOppdragEndpoint;
    private URI lagreVurderingEndpoint;
    private URI hentRisikoklassifiseringEndpoint;
    private RestClient restClient;

    FpriskTjeneste() {
        // CDI
    }

    @Inject
    public FpriskTjeneste(RestClient restClient) {
        this.restClient = restClient;
        var fpriskEndpoint = RestConfig.contextPathFromAnnotation(FpriskTjeneste.class);
        this.hentRisikoklassifiseringEndpoint = toUri(fpriskEndpoint, "/api/risikovurdering/hentResultat");
        this.lagreVurderingEndpoint = toUri(fpriskEndpoint, "/api/risikovurdering/lagreVurdering");
        this.sendOppdragEndpoint = toUri(fpriskEndpoint, "/api/risikovurdering/startRisikovurdering");
    }

    /**
     * Henter resultat av risikoklassifisering og evt vurdering og faresignaler fra fprisk
     * @param request
     * @return RisikovurderingResultatDto som inneholder både risikoklasse, vurdering og faresignaler
     */
    public Optional<RisikovurderingResultatDto> hentFaresignalerForBehandling(HentRisikovurderingDto request) {
        Objects.requireNonNull(request, "request");
        try {
            var rrequest = RestRequest.newPOSTJson(request, hentRisikoklassifiseringEndpoint, FpriskTjeneste.class);
            return restClient.sendReturnOptional(rrequest, RisikovurderingResultatDto.class);
        } catch (Exception e) {
            LOG.warn("Klarte ikke hente faresignaler fra fprisk", e);
            return Optional.empty();
        }
    }

    /**
     * Sender risikovurdering til fprisk
     * @param request
     */
    protected void sendRisikovurderingTilFprisk(LagreFaresignalVurderingDto request) {
        Objects.requireNonNull(request, "request");
        try {
            var rrequest = RestRequest.newPOSTJson(request, lagreVurderingEndpoint, FpriskTjeneste.class);
            restClient.sendReturnOptional(rrequest, String.class);
        } catch (Exception e) {
            LOG.warn("Klarte ikke lagre risikovurdering i fprisk", e);
            throw e;
        }
    }

    /**
     * Sender oppdrag om risikoklassifisering til fprisk
     * @param request
     */
    protected void sendRisikoklassifiseringsoppdrag(RisikovurderingRequestDto request) {
        Objects.requireNonNull(request, "request");
        try {
            var rrequest = RestRequest.newPOSTJson(request, sendOppdragEndpoint, FpriskTjeneste.class);
            restClient.sendReturnOptional(rrequest, String.class);
        } catch (Exception e) {
            // Feil hardt her når vi har verifisert en periode i prod at ting fungerer fint
            LOG.warn("Klarte ikke sende risikovurdering til fprisk", e);
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
