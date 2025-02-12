package no.nav.foreldrepenger.web.app.rest;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.web.app.konfig.ApiConfig;

public final class ResourceLinks {

    private static final Environment ENV = Environment.current();

    private ResourceLinks() {
    }

    public static ResourceLink get(String path, String rel) {
        return get(path, rel, null);
    }

    public static ResourceLink get(String path, String rel, Object queryParams) {
        var href = addPathPrefix(path);
        var query = toQuery(queryParams);
        return ResourceLink.get(href + query, rel);
    }

    public static ResourceLink post(String path, String rel) {
        return post(path, rel, null);
    }

    public static ResourceLink post(String path, String rel, Object requestPayload) {
        var href = addPathPrefix(path);
        return ResourceLink.post(href, rel, requestPayload);
    }

    public static String addPathPrefix(String path) {
        var contextPath = ENV.getProperty("context.path", "/fpsak");
        var apiUri = ApiConfig.API_URI;
        return contextPath + apiUri + path;
    }

    public static String toQuery(Object queryParams) {
        if (queryParams != null) {
            var mapper = new ObjectMapper();
            var mappedQueryParams = mapper.convertValue(queryParams, UriFormat.class).toString();
            if (!mappedQueryParams.isEmpty()) {
                return String.join("", "?", mappedQueryParams);
            }
        }
        return "";
    }

    private static class UriFormat {

        private final StringBuilder builder = new StringBuilder();

        @JsonAnySetter
        public void addToUri(String name, Object property) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(name).append("=").append(property);
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }
}
