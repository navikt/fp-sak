package no.nav.foreldrepenger.web.app.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Representerer en link til en resource/action i en HATEOAS response.
 *
 * @see https://restfulapi.net/hateoas/
 * @see https://tools.ietf.org/html/rfc5988
 */
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class ResourceLink {

    public enum HttpMethod {
        DELETE,
        GET,
        PATCH,
        POST,
        PUT,
    }

    @JsonProperty("href")
    @NotNull
    private URI href;

    /**
     * Link relationship type.
     */
    @JsonProperty("rel")
    @NotNull
    private String rel;

    @JsonProperty("requestPayload")
    private Object requestPayload;

    /**
     * Http Method type.
     */
    @JsonProperty("type")
    @NotNull
    private HttpMethod type;

    /**
     * Ctor lager default GET link.
     */
    public ResourceLink(String href, String rel) {
        this(href, rel, HttpMethod.GET);
    }

    public ResourceLink(String href, String rel, HttpMethod type) {
        try {
            this.href = new URI(href);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        this.rel = rel;
        this.type = type;
    }

    public ResourceLink(String href, String rel, HttpMethod type, Object requestPayload) {
        this(href, rel, type);
        this.requestPayload = requestPayload;
    }

    public ResourceLink(URI href, String rel, HttpMethod type) {
        this.href = href;
        this.rel = rel;
        this.type = type;
    }

    @SuppressWarnings("unused")
    private ResourceLink() { // NOSONAR
        this((URI) null, null, null); // for Jackson
    }

    public static ResourceLink get(String href, String rel) {
        return new ResourceLink(href, rel, HttpMethod.GET, null);
    }

    public static ResourceLink get(String href, String rel, Object queryParams) {
        var uri = new StringBuilder();
        uri.append(href);
        if (queryParams != null) {
            uri.append("?");
            uri.append(convertObjectToQueryString(queryParams));
        }
        return new ResourceLink(uri.toString(), rel, HttpMethod.GET, null);
    }

    public static ResourceLink post(String href, String rel, Object requestPayload) {
        return new ResourceLink(href, rel, HttpMethod.POST, requestPayload);
    }

    private static String convertObjectToQueryString(Object object) {
        var mapper = new ObjectMapper();
        return mapper.convertValue(object, UriFormat.class).toString();
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !this.getClass().equals(obj.getClass())) {
            return false;
        }
        var other = (ResourceLink) obj;
        return Objects.equals(this.href, other.href)
            && Objects.equals(this.rel, other.rel)
            && Objects.equals(this.type, other.type);
    }

    public URI getHref() {
        return href;
    }

    public String getRel() {
        return rel;
    }

    public Object getRequestPayload() {
        return requestPayload;
    }

    public HttpMethod getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(href, rel, type);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + type + " " + href + " [" + rel + "]>";
    }

}
