package no.nav.foreldrepenger.web.app.konfig;

import io.swagger.v3.oas.integration.GenericOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;
import no.nav.vedtak.exception.TekniskException;
import org.glassfish.jersey.server.ServerProperties;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationPath(ApiConfig.API_URI)
public class ApiConfig extends Application {

    private static final Environment ENV = Environment.current();

    public static final String API_URI = "/api";

    public ApiConfig() {
        var oas = new OpenAPI();
        var info = new Info()
            .title("FPSAK - Foreldrepenger, engangsstønad og svangerskapspenger")
            .version(Optional.ofNullable(ENV.imageName()).orElse("1.0"))
            .description("REST grensesnitt for FPSAK.");

        oas.info(info).addServersItem(new Server().url(ENV.getProperty("context.path", "/fpsak")));
        var oasConfig = new SwaggerConfiguration()
            .openAPI(oas)
            .prettyPrint(true)
            .resourceClasses(Stream.of(RestImplementationClasses.getImplementationClasses(), RestImplementationClasses.getForvaltningClasses())
                .flatMap(Collection::stream).map(Class::getName).collect(Collectors.toSet()));

        try {
            new GenericOpenApiContextBuilder<>()
                .openApiConfiguration(oasConfig)
                .buildContext(true)
                .read();
        } catch (OpenApiConfigurationException e) {
            throw new TekniskException("OPEN-API", e.getMessage(), e);
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
