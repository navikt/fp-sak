package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingRestTjenestePathHack1;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.FagsakRestTjeneste;

public final class Redirect {
    private static final Logger LOG = LoggerFactory.getLogger(Redirect.class);

    private Redirect() {
        // no ctor
    }

    public static Response tilBehandlingPollStatus(HttpServletRequest request, UUID behandlingUuid, Optional<String> gruppeOpt) throws URISyntaxException {
        UriBuilder uriBuilder = getUriBuilder(request)
            .path(BehandlingRestTjenestePathHack1.STATUS_PATH)
            .queryParam(UuidDto.NAME, behandlingUuid);
        gruppeOpt.ifPresent(s -> uriBuilder.queryParam("gruppe", s));
        return Response.accepted().location(honorXForwardedProto(request, uriBuilder.build())).build();
    }

    public static Response tilBehandlingPollStatus(HttpServletRequest request,UUID behandlingUuid) throws URISyntaxException {
        return tilBehandlingPollStatus(request, behandlingUuid, Optional.empty());
    }

    public static Response tilBehandlingEllerPollStatus(HttpServletRequest request, UUID behandlingUuid, AsyncPollingStatus status) throws URISyntaxException {
        var uriBuilder = getUriBuilder(request)
            .path(BehandlingRestTjenestePathHack1.BEHANDLING_PATH)
            .queryParam(UuidDto.NAME, behandlingUuid);
        return buildResponse(request, status, uriBuilder.build());
    }

    public static Response tilFagsakPollStatus(HttpServletRequest request, Saksnummer saksnummer, Optional<String> gruppeOpt) throws URISyntaxException {
        var uriBuilder = getUriBuilder(request)
            .path(FagsakRestTjeneste.FAGSAK_PATH)
            .queryParam("saksnummer", saksnummer.getVerdi());
        gruppeOpt.ifPresent(s -> uriBuilder.queryParam("gruppe", s));
        return Response.accepted().location(honorXForwardedProto(request, uriBuilder.build())).build();
    }

    public static Response tilFagsakEllerPollStatus(HttpServletRequest request, Saksnummer saksnummer, AsyncPollingStatus status) throws URISyntaxException {
        var uriBuilder = getUriBuilder(request)
            .path(FagsakRestTjeneste.FAGSAK_PATH)
            .queryParam("saksnummer", saksnummer.getVerdi());
        return buildResponse(request, status, uriBuilder.build());
    }

    private static UriBuilder getUriBuilder(HttpServletRequest request) {
        UriBuilder uriBuilder = request == null || request.getContextPath() == null ? UriBuilder.fromUri("") : UriBuilder.fromUri(URI.create(request.getContextPath()));
        Optional.ofNullable(request).map(HttpServletRequest::getServletPath).ifPresent(c -> uriBuilder.path(c));
        return uriBuilder;
    }

    private static Response buildResponse(HttpServletRequest request, AsyncPollingStatus status, URI resultatUri) throws URISyntaxException {
        var uri = honorXForwardedProto(request, resultatUri);
        if (status != null) {
            // sett alltid resultat-location i tilfelle timeout på klient
            status.setLocation(uri);
            return Response.status(status.getStatus().getHttpStatus()).entity(status).build();
        } else {
            return Response.status(Response.Status.SEE_OTHER).location(uri).build();
        }
    }

    private static URI honorXForwardedProto(HttpServletRequest request, URI location) throws URISyntaxException {
        URI newLocation = null;
        if (relativLocationAndRequestAvailable(location)) {
            String xForwardedProto = getXForwardedProtoHeader(request);

            if (mismatchedScheme(xForwardedProto, request)) {
                var path = location.toString();
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                var baseUri = new URI(request.getRequestURI());
                try {
                    var rewritten = new URI(xForwardedProto, baseUri.getSchemeSpecificPart(), baseUri.getFragment())
                        .resolve(path);
                    LOG.debug("Rewrote URI from '{}' to '{}'", location, rewritten);
                    newLocation = rewritten;
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
            }
        }
        return newLocation != null ? newLocation : (erKallFraGcp(request) ? location : leggTilBaseUri(location));
    }

    private static boolean erKallFraGcp(HttpServletRequest request) {
        var xForwardedHost = request.getHeader("X-Forwarded-Host");
        return xForwardedHost != null && xForwardedHost.contains("fss-pub.nais.io");
    }

    private static boolean relativLocationAndRequestAvailable(URI location) {
        return location != null && !location.isAbsolute();
    }

    /**
     * @return http, https or null
     */
    private static String getXForwardedProtoHeader(HttpServletRequest httpRequest) {
        var xForwardedProto = httpRequest.getHeader("X-Forwarded-Proto");
        if ("https".equalsIgnoreCase(xForwardedProto) ||
            "http".equalsIgnoreCase(xForwardedProto)) {
            return xForwardedProto;
        }
        return null;
    }

    private static boolean mismatchedScheme(String xForwardedProto, HttpServletRequest httpRequest) {
        return xForwardedProto != null &&
            !xForwardedProto.equalsIgnoreCase(httpRequest.getScheme());
    }

    @SuppressWarnings("resource")
    private static URI leggTilBaseUri(URI resultatUri) {
        // tvinger resultatUri til å være en absolutt URI (passer med Location Header og Location felt når kommer i payload)
        var response = Response.noContent().location(resultatUri).build();
        return response.getLocation();
    }
}
