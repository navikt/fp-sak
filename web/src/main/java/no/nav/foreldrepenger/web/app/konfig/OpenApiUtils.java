package no.nav.foreldrepenger.web.app.konfig;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Application;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.ObjectMapperFactory;
import io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import no.nav.foreldrepenger.web.app.jackson.JacksonJsonConfig;
import no.nav.openapi.spec.utils.openapi.DiscriminatorModelConverter;
import no.nav.openapi.spec.utils.openapi.EnumVarnamesConverter;
import no.nav.openapi.spec.utils.openapi.JsonSubTypesModelConverter;
import no.nav.openapi.spec.utils.openapi.NoJsonSubTypesAnnotationIntrospector;
import no.nav.openapi.spec.utils.openapi.PrefixStrippingFQNTypeNameResolver;
import no.nav.openapi.spec.utils.openapi.RefToClassLookup;
import no.nav.openapi.spec.utils.openapi.RegisteredSubtypesModelConverter;
import no.nav.vedtak.exception.TekniskException;

public class OpenApiUtils {

    private final SwaggerConfiguration swaggerConfiguration;
    private final Application application;

    private OpenApiUtils(SwaggerConfiguration swaggerConfiguration, Application application) {
        this.swaggerConfiguration = swaggerConfiguration;
        this.application = application;
    }

    public static OpenApiUtils openApiConfigFor(Info info, String contextPath, Application application) {
        var oas = new OpenAPI()
            .openapi("3.1.1")
            .info(info)
            .addServersItem(new Server().url(contextPath));
        var swaggerConfiguration = new SwaggerConfiguration()
            .id(idFra(application))
            .openAPI(oas)
            .prettyPrint(true)
            .scannerClass(JaxrsAnnotationScanner.class.getName());
        return new OpenApiUtils(swaggerConfiguration, application);
    }

    public OpenApiUtils registerClasses(Collection<Class<?>> resourceClasses) {
        swaggerConfiguration.resourceClasses(resourceClasses.stream().map(Class::getName).collect(Collectors.toSet()));
        return this;
    }

    public void buildOpenApiContext() {
        try {
            new JaxrsOpenApiContextBuilder<>()
                .ctxId(idFra(application))
                .application(application)
                .openApiConfiguration(swaggerConfiguration)
                .buildContext(true);
        } catch (OpenApiConfigurationException e) {
            throw new TekniskException("OPEN-API", e.getMessage(), e);
        }
    }

    private static String idFra(Application application) {
        return "openapi.context.id.servlet." + application.getClass().getName();
    }

    public static void settOppForTypegenereringFrontend() {
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
        return JsonMapper.builder(ObjectMapperFactory.createJson().getFactory())
            // OpenApi-spec som blir generert er ikke alltid konsekvent på rekkefølgen til properties.
            // Ved å skru på disse flaggene blir output deterministic og det blir enklere å se hva som faktisk er diff fra forrige typegenerering
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .enable(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST)
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            // Fjern alle annotasjoner om JsonSubTypes. Hvis disse er med i generasjon av openapi spec får vi sirkulære avhengigheter.
            // Det skjer ved at superklassen sier den har "oneOf" arvingene sine. Mens en arving sier den har "allOf" forelderen sin.
            // Ved å fjerne jsonSubType annotasjoner får vi heller en enveis-lenke der superklassen definerer arvingene sine med "oneOf".
            .annotationIntrospector(new NoJsonSubTypesAnnotationIntrospector())
            .build();
    }
}
