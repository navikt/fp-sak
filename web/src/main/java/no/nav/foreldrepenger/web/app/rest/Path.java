package no.nav.foreldrepenger.web.app.rest;

import jakarta.ws.rs.core.UriBuilder;

public record Path(String uriTemplate, PathParamMap parameters) {
    public static Path of(String uriTemplate, PathParamMap parameters) {
        return new Path(uriTemplate, parameters);
    }

    public String build() {
        return UriBuilder.fromPath(uriTemplate).buildFromMap(parameters).toString();
    }
}
