package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingRestTjenestePathHack1;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.FagsakRestTjeneste;

public final class Redirect {
    private static final Logger log = LoggerFactory.getLogger(Redirect.class);

    private Redirect() {
        // no ctor
    }

    public static Response tilBehandlingPollStatus(UUID behandlingUuid, Optional<String> gruppeOpt) {
        UriBuilder uriBuilder = UriBuilder.fromPath(BehandlingRestTjenestePathHack1.STATUS_PATH);
        uriBuilder.queryParam(UuidDto.NAME, behandlingUuid);
        gruppeOpt.ifPresent(s -> uriBuilder.queryParam("gruppe", s));
        return Response.accepted().location(honorXForwardedProto(uriBuilder.build())).build();
    }

    public static Response tilBehandlingPollStatus(UUID behandlingUuid) {
        return tilBehandlingPollStatus(behandlingUuid, Optional.empty());
    }

    public static Response tilBehandlingEllerPollStatus(UUID behandlingUuid, AsyncPollingStatus status) throws URISyntaxException {
        UriBuilder uriBuilder = UriBuilder.fromPath(BehandlingRestTjenestePathHack1.BEHANDLING_PATH);
        uriBuilder.queryParam(UuidDto.NAME, behandlingUuid);
        return buildResponse(status, uriBuilder.build());
    }

    public static Response tilFagsakPollStatus(Saksnummer saksnummer, Optional<String> gruppeOpt) throws URISyntaxException {
        UriBuilder uriBuilder = UriBuilder.fromPath(FagsakRestTjeneste.STATUS_PATH);
        uriBuilder.queryParam("saksnummer", saksnummer.getVerdi());
        gruppeOpt.ifPresent(s -> uriBuilder.queryParam("gruppe", s));
        return Response.accepted().location(honorXForwardedProto(uriBuilder.build())).build();
    }

    public static Response tilFagsakEllerPollStatus(Saksnummer saksnummer, AsyncPollingStatus status) throws URISyntaxException {
        UriBuilder uriBuilder = UriBuilder.fromPath(FagsakRestTjeneste.FAGSAK_PATH);
        uriBuilder.queryParam("saksnummer", saksnummer.getVerdi());
        return buildResponse(status, uriBuilder.build());
    }

    private static Response buildResponse(AsyncPollingStatus status, URI resultatUri) throws URISyntaxException {
        URI uri = honorXForwardedProto(resultatUri);
        if (status != null) {
            // sett alltid resultat-location i tilfelle timeout på klient
            status.setLocation(uri);
            return Response.status(status.getStatus().getHttpStatus()).entity(status).build();
        } else {
            return Response.seeOther(uri).build();
        }
    }

    /**
     * @see org.jboss.resteasy.specimpl.ResponseBuilderImpl#location(URI)
     * @see URI#create(String)
     */
    private static URI honorXForwardedProto(URI location) {
        URI newLocation = null;
        if (relativLocationAndRequestAvailable(location)) {
            HttpRequest httpRequest = ResteasyProviderFactory.getContextData(HttpRequest.class);
            String xForwardedProto = getXForwardedProtoHeader(httpRequest);

            if (mismatchedScheme(xForwardedProto, httpRequest)) {
                String path = location.toString();
                if (path.startsWith("/")) { // NOSONAR
                    path = path.substring(1); // NOSONAR
                }
                URI baseUri = httpRequest.getUri().getBaseUri();
                try {
                    URI rewritten = new URI(xForwardedProto, baseUri.getSchemeSpecificPart(), baseUri.getFragment())
                        .resolve(path);
                    log.debug("Rewrote URI from '{}' to '{}'", location, rewritten);
                    newLocation = rewritten;
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
            }
        }
        return newLocation != null ? newLocation : leggTilBaseUri(location);
    }

    private static boolean relativLocationAndRequestAvailable(URI location) {
        return location != null &&
            !location.isAbsolute() &&
            ResteasyProviderFactory.getContextData(HttpRequest.class) != null;
    }

    /**
     * @return http, https or null
     */
    private static String getXForwardedProtoHeader(HttpRequest httpRequest) {
        String xForwardedProto = httpRequest.getHttpHeaders().getHeaderString("X-Forwarded-Proto");
        if (xForwardedProto != null &&
            ("https".equalsIgnoreCase(xForwardedProto) ||
                "http".equalsIgnoreCase(xForwardedProto))) {
            return xForwardedProto;
        }
        return null;
    }

    private static boolean mismatchedScheme(String xForwardedProto, HttpRequest httpRequest) {
        return xForwardedProto != null &&
            !xForwardedProto.equalsIgnoreCase(httpRequest.getUri().getBaseUri().getScheme());
    }

    @SuppressWarnings("resource")
    private static URI leggTilBaseUri(URI resultatUri) {
        // tvinger resultatUri til å være en absolutt URI (passer med Location Header og Location felt når kommer i payload)
        Response response = Response.noContent().location(resultatUri).build();
        return response.getLocation();
    }
}
