package no.nav.foreldrepenger.web.app.konfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import org.glassfish.jersey.server.ServerProperties;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.models.info.Info;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;

@ApplicationPath(ForvaltningApiConfig.API_URI)
public class ForvaltningApiConfig extends Application {

    private static final Environment ENV = Environment.current();

    public static final String API_URI = "/forvaltning/api";

    public ForvaltningApiConfig() {
        var info = new Info()
            .title("FPSAK Forvaltning - Foreldrepenger, engangsstønad og svangerskapspenger")
            .version(Optional.ofNullable(ENV.imageName()).orElse("1.0"))
            .description("REST grensesnitt for FP-swagger.");
        var contextPath = ENV.getProperty("context.path", "/fpsak");

        OpenApiUtils.openApiConfigFor(info, contextPath, this)
            .registerClasses(RestImplementationClasses.getForvaltningClasses())
            .buildOpenApiContext();
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // Krever autentisert og medlem av DRIFT
        classes.add(ForvaltningAuthorizationFilter.class);
        // forvaltning/swagger
        classes.addAll(RestImplementationClasses.getForvaltningClasses());
        // swagger
        classes.add(OpenApiResource.class);

        // Applikasjonsoppsett
        classes.addAll(FellesConfigClasses.getFellesConfigClasses());

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
