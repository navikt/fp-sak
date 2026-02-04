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

@ApplicationPath(ApiConfig.API_URI)
public class ApiConfig extends Application {

    private static final Environment ENV = Environment.current();
    private static final boolean ER_PROD = ENV.isProd();

    public static final String API_URI = "/api";

    public ApiConfig() {
        if (!ER_PROD) {
            registerOpenApi();
        }
    }

    private void registerOpenApi() {
        var info = new Info()
            .title("FPSAK - Foreldrepenger, engangsstønad og svangerskapspenger")
            .version(Optional.ofNullable(ENV.imageName()).orElse("1.0"))
            .description("REST grensesnitt for FP-frontend.");
        var contextPath = ENV.getProperty("context.path", "/fpsak");

        OpenApiUtils.settOppForTypegenereringFrontend();

        OpenApiUtils.openApiConfigFor(info, contextPath, this)
            .registerClasses(RestImplementationClasses.getImplementationClasses())
            .buildOpenApiContext();
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // eksponert grensesnitt mot frontend
        classes.addAll(RestImplementationClasses.getImplementationClasses());
        // eksponert grensesnitt mot andre applikasjoner interne + noen få eksterne
        classes.addAll(RestImplementationClasses.getServiceClasses());
        if (!ER_PROD) {
            // swagger
            classes.add(OpenApiResource.class);
        }
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
