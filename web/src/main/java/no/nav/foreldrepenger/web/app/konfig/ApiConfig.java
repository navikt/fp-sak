package no.nav.foreldrepenger.web.app.konfig;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.SerializationFeature;

import com.fasterxml.jackson.databind.json.JsonMapper;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.ObjectMapperFactory;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import no.nav.openapi.spec.utils.openapi.DiscriminatorModelConverter;
import no.nav.openapi.spec.utils.openapi.JsonSubTypesModelConverter;

import no.nav.openapi.spec.utils.openapi.NoJsonSubTypesAnnotationIntrospector;
import no.nav.openapi.spec.utils.openapi.PrefixStrippingFQNTypeNameResolver;
import no.nav.openapi.spec.utils.openapi.RefToClassLookup;
import no.nav.openapi.spec.utils.openapi.RegisteredSubtypesModelConverter;

import org.glassfish.jersey.server.ServerProperties;

import io.swagger.v3.oas.integration.GenericOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;
import no.nav.vedtak.exception.TekniskException;

import static no.nav.foreldrepenger.web.app.jackson.JacksonJsonConfig.getJsonTypeNameClasses;

@ApplicationPath(ApiConfig.API_URI)
public class ApiConfig extends Application {

    private static final Environment ENV = Environment.current();

    public static final String API_URI = "/api";

    public ApiConfig() {
        var info = new Info()
            .title("FPSAK - Foreldrepenger, engangsstønad og svangerskapspenger")
            .version(Optional.ofNullable(ENV.imageName()).orElse("1.0"))
            .description("REST grensesnitt for FPSAK.");
        var server = new Server().url(ENV.getProperty("context.path", "/fpsak"));

        try {
            var oas = new OpenAPI();

            oas.info(info).addServersItem(server);
            var oasConfig = new SwaggerConfiguration()
                .openAPI(oas)
                .prettyPrint(true)
                .resourcePackages(Collections.singleton("no.nav.foreldrepenger"))
                .resourceClasses(Stream.of(RestImplementationClasses.getImplementationClasses(), RestImplementationClasses.getForvaltningClasses())
                    .flatMap(Collection::stream).map(Class::getName).collect(Collectors.toSet()));

            // Påfølgende ModelConverts oppsett er tilpasset fra K9 sin openapi-spec-utils: https://github.com/navikt/openapi-spec-utils

            // Denne gjør at enums trekkes ut som egne typer istedenfor inline
            ModelResolver.enumsAsRef = true;
            ModelConverters.reset();

            // Her ber vi den om å inkludere pakkenavn i typenavnet. Da risikerer vi ikke kollisjon ved like typenavn. fqn = fully-qualified-name.
            // Ved kollisjon vil den som er sist prosessert overskrive alle tidligere.
            var typeNameResolver =new PrefixStrippingFQNTypeNameResolver("no.nav.foreldrepenger.web.app.", "no.nav.");
            typeNameResolver.setUseFqn(true);

            ModelConverters.getInstance().addConverter(new ModelResolver(lagObjectMapperUtenJsonSubTypeAnnotasjoner(),  typeNameResolver));

            Set<Class<?>> registeredSubtypes = allJsonTypeNameClasses();
            ModelConverters.getInstance().addConverter(new RegisteredSubtypesModelConverter(registeredSubtypes));
            ModelConverters.getInstance().addConverter(new JsonSubTypesModelConverter());
            ModelConverters.getInstance().addConverter(new DiscriminatorModelConverter(new RefToClassLookup()));

            var context = new GenericOpenApiContextBuilder<>()
                .openApiConfiguration(oasConfig)
                .buildContext(false);

            context.init();
            context.read();


        } catch (OpenApiConfigurationException e) {
            throw new TekniskException("OPEN-API", e.getMessage(), e);
        }
    }

    private static ObjectMapper lagObjectMapperUtenJsonSubTypeAnnotasjoner() {
        final var om = ObjectMapperFactory.createJson();
        // Fjern alle annotasjoner om JsonSubTypes. Hvis disse er med i generasjon av openapi spec får vi sirkulære avhengigheter.
        // Det skjer ved at superklassen sier den har "oneOf" arvingene sine. Mens en arving sier den har "allOf" forelderen sin.
        // Ved å fjerne jsonSubType annotasjoner får vi heller en enveis-lenke der superklassen definerer arvingene sine med "oneOf".
        om.setAnnotationIntrospector(new NoJsonSubTypesAnnotationIntrospector());
        return withDeterministicOutput(om);
    }

    protected static ObjectMapper withDeterministicOutput(final ObjectMapper om) {
        return JsonMapper.builder(om.getFactory())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();
    }

    public static Set<Class<?>> allJsonTypeNameClasses() {
        final Collection<Class<?>> restClasses = RestImplementationClasses.getImplementationClasses();

        final Set<Class<?>> scanClasses = new LinkedHashSet<>(restClasses);

        return scanClasses
            .stream()
            .map(c -> {
                try {
                    return c.getProtectionDomain().getCodeSource().getLocation().toURI();
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Ikke en URI for klasse: " + c, e);
                }
            })
            .distinct()
            .flatMap(uri -> getJsonTypeNameClasses(uri).stream())
            .collect(Collectors.toUnmodifiableSet());

    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // eksponert grensesnitt
        classes.addAll(RestImplementationClasses.getImplementationClasses());
        // forvaltning/swagger
        classes.addAll(RestImplementationClasses.getForvaltningClasses());

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
