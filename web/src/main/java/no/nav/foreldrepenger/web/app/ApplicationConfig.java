package no.nav.foreldrepenger.web.app;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import no.nav.foreldrepenger.web.app.exceptions.ConstraintViolationMapper;
import no.nav.foreldrepenger.web.app.exceptions.GeneralRestExceptionMapper;
import no.nav.foreldrepenger.web.app.exceptions.JsonMappingExceptionMapper;
import no.nav.foreldrepenger.web.app.exceptions.JsonParseExceptionMapper;
import no.nav.foreldrepenger.web.app.jackson.JacksonJsonConfig;
import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;
import no.nav.foreldrepenger.web.app.tjenester.datavarehus.DatavarehusAdminRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningBehandlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningFagsakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningOpptjeningRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningStegRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningTekniskRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningUttakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningUttrekkRestTjeneste;


@ApplicationPath(ApplicationConfig.API_URI)
public class ApplicationConfig extends Application {

    public static final String API_URI = "/api";

    public ApplicationConfig() {
        OpenAPI oas = new OpenAPI();
        Info info = new Info()
            .title("FPSAK - Foreldrepenger, engangsstønad og svangerskapspenger")
            .version("1.0")
            .description("REST grensesnitt for FPSAK.");

        oas.info(info)
            .addServersItem(new Server()
                .url("/fpsak"));
        SwaggerConfiguration oasConfig = new SwaggerConfiguration()
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
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.addAll(new RestImplementationClasses().getImplementationClasses());

        //UtilTjenester for uttrekk fra registre
        classes.add(DatavarehusAdminRestTjeneste.class);
        classes.add(ForvaltningFagsakRestTjeneste.class);
        classes.add(ForvaltningTekniskRestTjeneste.class);
        classes.add(ForvaltningUttrekkRestTjeneste.class);
        classes.add(ForvaltningOpptjeningRestTjeneste.class);
        classes.add(ForvaltningUttakRestTjeneste.class);
        classes.add(ForvaltningBehandlingRestTjeneste.class);
        classes.add(ForvaltningStegRestTjeneste.class);

        classes.add(OpenApiResource.class);

        classes.add(ConstraintViolationMapper.class);
        classes.add(JsonMappingExceptionMapper.class);
        classes.add(JsonParseExceptionMapper.class);
        classes.add(GeneralRestExceptionMapper.class);
        classes.add(JacksonJsonConfig.class);

        return Collections.unmodifiableSet(classes);
    }
}
