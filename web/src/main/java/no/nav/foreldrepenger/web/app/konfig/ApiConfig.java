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

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.ObjectMapperFactory;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.models.info.Info;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.web.app.jackson.JacksonJsonConfig;
import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;
import no.nav.openapi.spec.utils.openapi.DiscriminatorModelConverter;
import no.nav.openapi.spec.utils.openapi.EnumVarnamesConverter;
import no.nav.openapi.spec.utils.openapi.JsonSubTypesModelConverter;
import no.nav.openapi.spec.utils.openapi.NoJsonSubTypesAnnotationIntrospector;
import no.nav.openapi.spec.utils.openapi.PrefixStrippingFQNTypeNameResolver;
import no.nav.openapi.spec.utils.openapi.RefToClassLookup;
import no.nav.openapi.spec.utils.openapi.RegisteredSubtypesModelConverter;

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

        settOppForTypegenereringFrontend();

        OpenApiUtils.openApiConfigFor(info, contextPath, this)
            .registerClasses(RestImplementationClasses.getImplementationClasses())
            .buildOpenApiContext();
    }

    private static void settOppForTypegenereringFrontend() {
        // Påfølgende ModelConverts oppsett er tilpasset fra K9 sin openapi-spec-utils: https://github.com/navikt/openapi-spec-utils

        // Denne gjør at enums trekkes ut som egne typer istedenfor inline
        ModelResolver.enumsAsRef = true;
        ModelConverters.reset();

        // Her ber vi den om å inkludere pakkenavn i typenavnet. Da risikerer vi ikke kollisjon ved like typenavn. fqn = fully-qualified-name.
        // Ved kollisjon vil den som er sist prosessert overskrive alle tidligere.
        var typeNameResolver =new PrefixStrippingFQNTypeNameResolver("no.nav.foreldrepenger.web.app.", "no.nav.");
        typeNameResolver.setUseFqn(true);

        ModelConverters.getInstance().addConverter(new ModelResolver(lagObjectMapperUtenJsonSubTypeAnnotasjoner(),  typeNameResolver));

        Set<Class<?>> registeredSubtypes = JacksonJsonConfig.allJsonTypeNameClasses();
        ModelConverters.getInstance().addConverter(new RegisteredSubtypesModelConverter(registeredSubtypes));
        ModelConverters.getInstance().addConverter(new JsonSubTypesModelConverter());
        ModelConverters.getInstance().addConverter(new DiscriminatorModelConverter(new RefToClassLookup()));
        ModelConverters.getInstance().addConverter(new EnumVarnamesConverter());
    }

    private static ObjectMapper lagObjectMapperUtenJsonSubTypeAnnotasjoner() {
        final var om = JsonMapper.builder(ObjectMapperFactory.createJson().getFactory())
            // OpenApi-spec som blir generert er ikke alltid konsekvent på rekkefølgen til properties.
            // Ved å skru på disse flaggene blir output deterministic og det blir enklere å se hva som faktisk er diff fra forrige typegenerering
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .enable(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST)
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();
        // Fjern alle annotasjoner om JsonSubTypes. Hvis disse er med i generasjon av openapi spec får vi sirkulære avhengigheter.
        // Det skjer ved at superklassen sier den har "oneOf" arvingene sine. Mens en arving sier den har "allOf" forelderen sin.
        // Ved å fjerne jsonSubType annotasjoner får vi heller en enveis-lenke der superklassen definerer arvingene sine med "oneOf".
        om.setAnnotationIntrospector(new NoJsonSubTypesAnnotationIntrospector());
        return om;
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
