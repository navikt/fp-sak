package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.UriBuilder;

public class HistorikkRequestPath {

    private static final String HENT_DOK_PATH = "/dokument/hent-dokument";

    private HistorikkRequestPath() {
    }

    public static URI getRequestPath(HttpServletRequest request) {
        // FIXME XSS valider requestURL eller bruk relativ URL
        if (request == null) {
            return null;
        }
        var stringBuilder = new StringBuilder();

        stringBuilder.append(request.getScheme())
            .append("://")
            .append(request.getLocalName())
            .append(":")
            .append(request.getLocalPort());

        stringBuilder.append(request.getContextPath())
            .append(request.getServletPath());
        return UriBuilder.fromUri(stringBuilder.toString()).path(HENT_DOK_PATH).build();
    }
}
