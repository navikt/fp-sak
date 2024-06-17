package no.nav.foreldrepenger.web.app.rest;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.web.app.konfig.ApiConfig;

public final class ResourceLinks {

    private static final Environment ENV = Environment.current();

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
        var contextPath = ENV.getProperty("context.path", "/fpsak");
        var apiUri = ApiConfig.API_URI;
        return contextPath + apiUri + path;
    }
}
