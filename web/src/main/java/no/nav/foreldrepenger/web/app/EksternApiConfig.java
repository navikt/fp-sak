package no.nav.foreldrepenger.web.app;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;

import io.swagger.v3.oas.models.info.Info;

import io.swagger.v3.oas.models.servers.Server;

import org.glassfish.jersey.server.ServerProperties;

import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;

@ApplicationPath(EksternApiConfig.API_URI)
public class EksternApiConfig extends Application {

    public static final String API_URI = "/ekstern/api";

    private static final String ID_PREFIX = "openapi.context.id.servlet.";

    public EksternApiConfig() {
        var oas = new OpenAPI();
        var info = new Info()
            .title("FPSAK - Foreldrepenger, engangsstønad og svangerskapspenger")
            .version("1.0")
            .description("REST grensesnitt for FPSAK eksterne brukere. Alle kall må authentiseres med en gyldig Azure OBO eller CC token.");

        oas.info(info)
            .addServersItem(new Server()
                .url("/fpsak"));
        var oasConfig = new SwaggerConfiguration()
            .id(ID_PREFIX + EksternApiConfig.class.getName())
            .openAPI(oas)
            .prettyPrint(true)
            //.scannerClass("io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner")
            .resourceClasses(RestImplementationClasses.getExternalIntegrationClasses().stream().map(Class::getName).collect(Collectors.toSet()));

        try {
            new JaxrsOpenApiContextBuilder<>()
                .ctxId(ID_PREFIX + EksternApiConfig.class.getName())
                .application(this)
                .openApiConfiguration(oasConfig)
                .buildContext(true)
                .read();
        } catch (OpenApiConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // eksterne integrasjoner
        classes.addAll(RestImplementationClasses.getExternalIntegrationClasses());

        // Applikasjonsoppsett
        classes.addAll(RestImplementationClasses.getFellesConfigClasses());

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
