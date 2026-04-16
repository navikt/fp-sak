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
import no.nav.foreldrepenger.web.app.jackson.JacksonJsonConfig;
import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;
import no.nav.vedtak.openapi.OpenApiUtils;
import no.nav.vedtak.server.rest.AuthenticationFilter;
import no.nav.vedtak.server.rest.GeneralRestExceptionMapper;
import no.nav.vedtak.server.rest.ValidationExceptionMapper;
import no.nav.vedtak.server.rest.jackson.Jackson2BasicFeature;

@ApplicationPath(ApiConfig.API_URI)
public class ApiConfig extends Application {

    private static final Environment ENV = Environment.current();
    private static final boolean ER_PROD = ENV.isProd();

    public static final String API_URI = "/api";

    public ApiConfig() {
        GeneralRestExceptionMapper.setLegacyFrontendInternFeil(true);
        if (!ER_PROD) {
            registerOpenApi();
        }
    }

    private void registerOpenApi() {
        var contextPath = ENV.getProperty("context.path", "/fpsak");
        LokalOpenApiUtils.settOppForTypegenereringFrontend();
        OpenApiUtils.setupOpenApi("FPSAK - Foreldrepenger, engangsstønad og svangerskapspenger",
            contextPath, RestImplementationClasses.getImplementationClasses(), this);
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
        // Applikasjonsoppsett - her er det brutt opp pga JsonSubTypes i mapper
        classes.add(AuthenticationFilter.class);
        classes.add(Jackson2BasicFeature.class);
        classes.add(JacksonJsonConfig.class);
        classes.add(ValidationExceptionMapper.class);
        classes.add(GeneralRestExceptionMapper.class);

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
