package no.nav.foreldrepenger.web.app.konfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import org.glassfish.jersey.server.ServerProperties;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;
import no.nav.vedtak.openapi.OpenApiUtils;
import no.nav.vedtak.server.rest.ForvaltningAuthorizationFilter;
import no.nav.vedtak.server.rest.FpRestJackson2Feature;

@ApplicationPath(ForvaltningApiConfig.API_URI)
public class ForvaltningApiConfig extends Application {

    private static final Environment ENV = Environment.current();

    public static final String API_URI = "/forvaltning/api";

    public ForvaltningApiConfig() {
        var contextPath = ENV.getProperty("context.path", "/fpsak");

        OpenApiUtils.setupOpenApi("FPSAK Forvaltning - Foreldrepenger, engangsstønad og svangerskapspenger",
                contextPath, RestImplementationClasses.getForvaltningClasses(), this);
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // Standard oppsett + tilgang: krav om autentisert og medlem av DRIFT
        classes.add(FpRestJackson2Feature.class);
        classes.add(ForvaltningAuthorizationFilter.class);
        // forvaltning/swagger
        classes.addAll(RestImplementationClasses.getForvaltningClasses());
        // swagger
        classes.add(OpenApiResource.class);

        return Collections.unmodifiableSet(classes);
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        // Ref Jersey doc
        properties.put(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        properties.put(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
        return properties;
    }

}
