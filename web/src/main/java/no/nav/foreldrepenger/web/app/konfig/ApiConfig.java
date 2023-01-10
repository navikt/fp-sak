package no.nav.foreldrepenger.web.app.konfig;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import no.nav.vedtak.exception.TekniskException;

import org.glassfish.jersey.server.ServerProperties;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;

@ApplicationPath(ApiConfig.API_URI)
public class ApiConfig extends Application {

    public static final String API_URI = "/api";
    private static final String ID_PREFIX = "openapi.context.id.servlet.";

    public ApiConfig() {
        var oas = new OpenAPI();
        var info = new Info()
            .title("FPSAK - Foreldrepenger, engangsstønad og svangerskapspenger")
            .version("1.0")
            .description("REST grensesnitt for FPSAK.");

        oas.info(info).addServersItem(new Server().url("/fpsak"));
        var oasConfig = new SwaggerConfiguration()
            .id(ID_PREFIX + ApiConfig.class.getName())
            .openAPI(oas)
            .prettyPrint(true)
            //.scannerClass("io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner")
            .resourceClasses(Stream.of(RestImplementationClasses.getImplementationClasses(), RestImplementationClasses.getForvaltningClasses())
                .flatMap(Collection::stream).map(Class::getName).collect(Collectors.toSet()));

        try {
            new JaxrsOpenApiContextBuilder<>()
                .ctxId(ID_PREFIX + ApiConfig.class.getName())
                .application(this)
                .openApiConfiguration(oasConfig)
                .buildContext(true)
                .read();
        } catch (OpenApiConfigurationException e) {
            throw new TekniskException("OPENAPI", e.getMessage(), e);
        }
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // eksponert grensesnitt
        classes.addAll(RestImplementationClasses.getImplementationClasses());
        // forvaltning/swagger
        classes.addAll(RestImplementationClasses.getForvaltningClasses());

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
