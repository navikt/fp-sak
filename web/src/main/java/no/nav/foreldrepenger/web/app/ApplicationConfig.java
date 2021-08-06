package no.nav.foreldrepenger.web.app;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import io.swagger.v3.jaxrs2.SwaggerSerializers;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import no.nav.foreldrepenger.web.app.exceptions.ConstraintViolationMapper;
import no.nav.foreldrepenger.web.app.exceptions.GenerellVLExceptionMapper;
import no.nav.foreldrepenger.web.app.exceptions.JsonMappingExceptionMapper;
import no.nav.foreldrepenger.web.app.exceptions.JsonParseExceptionMapper;
import no.nav.foreldrepenger.web.app.jackson.JacksonJsonConfig;
import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;
import no.nav.foreldrepenger.web.server.jetty.TimingFilter;

@ApplicationPath(ApplicationConfig.API_URI)
public class ApplicationConfig extends ResourceConfig {

    public static final String API_URI = "/api";

    public ApplicationConfig() {
        var oas = new OpenAPI();
        var info = new Info()
                .title("FPSAK - Foreldrepenger, engangsstønad og svangerskapspenger")
                .version("1.0")
                .description("REST grensesnitt for FPSAK.");

        oas.info(info)
                .addServersItem(new Server()
                        .url("/fpsak"));
        var oasConfig = new SwaggerConfiguration()
                .openAPI(oas)
                .prettyPrint(true)
                .scannerClass("io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner")
                .resourcePackages(Stream.of("no.nav")
                        .collect(Collectors.toSet()));

        try {
            new JaxrsOpenApiContextBuilder<>()
                    .openApiConfiguration(oasConfig)
                    .buildContext(true)
                    .read();
        } catch (OpenApiConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        register(SwaggerSerializers.class);
        register(OpenApiResource.class);
        register(JacksonJsonConfig.class);
        register(TimingFilter.class);

        registerClasses(new LinkedHashSet<>(RestImplementationClasses.getImplementationClasses()));
        registerClasses(new LinkedHashSet<>(RestImplementationClasses.getForvaltningClasses()));

        // Disse overstyrer tilsvarende fra jackson+jersey
        register(ConstraintViolationMapper.class);
        register(JsonMappingExceptionMapper.class);
        register(JsonParseExceptionMapper.class);
        // Map+Logg VLException + Alle andre
        register(GenerellVLExceptionMapper.class);

        property(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
    }

}
