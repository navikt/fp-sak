package no.nav.foreldrepenger.web.app.rest;

import no.nav.foreldrepenger.web.app.ApplicationConfig;
import no.nav.foreldrepenger.web.server.jetty.JettyWebKonfigurasjon;

public final class ResourceLinks {

    private ResourceLinks() {
    }

    public static ResourceLink get(String path, String rel) {
        return get(path, rel, null);
    }

    public static ResourceLink get(String path, String rel, Object dto) {
        var href = href(path);
        return ResourceLink.get(href, rel, dto);
    }

    public static ResourceLink post(String path, String rel) {
        return post(path, rel, null);
    }

    public static ResourceLink post(String path, String rel, Object dto) {
        var href = href(path);
        return ResourceLink.post(href, rel, dto);
    }

    private static String href(String path) {
        var contextPath = JettyWebKonfigurasjon.CONTEXT_PATH;
        var apiUri = ApplicationConfig.API_URI;
        return contextPath + apiUri + path;
    }
}
