package no.nav.foreldrepenger.web.app.rest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.web.app.ApplicationConfig;

@ApplicationScoped
public class ResourceLinks {

    private ContextPathProvider contextPathProvider;

    @Inject
    public ResourceLinks(ContextPathProvider contextPathProvider) {
        this.contextPathProvider = contextPathProvider;
    }

    ResourceLinks() {
        //CDI
    }

    public ResourceLink get(String path, String rel) {
        return get(path, rel, null);
    }

    public ResourceLink get(String path, String rel, Object dto) {
        var href = href(path);
        return ResourceLink.get(href, rel, dto);
    }

    public ResourceLink post(String path, String rel) {
        return post(path, rel, null);
    }

    public ResourceLink post(String path, String rel, Object dto) {
        var href = href(path);
        return ResourceLink.post(href, rel, dto);
    }

    private String href(String path) {
        var contextPath = contextPathProvider.get();
        var apiUri = ApplicationConfig.API_URI;
        return contextPath + apiUri + path;
    }
}
