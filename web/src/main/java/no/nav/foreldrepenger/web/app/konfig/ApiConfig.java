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


import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.jackson.TypeNameResolver;
import io.swagger.v3.core.util.ObjectMapperFactory;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import no.nav.openapi.spec.utils.openapi.DiscriminatorModelConverter;
import no.nav.openapi.spec.utils.openapi.JsonSubTypesModelConverter;
import no.nav.openapi.spec.utils.openapi.NoJsonSubTypesAnnotationIntrospector;

import no.nav.openapi.spec.utils.openapi.PrefixStrippingFQNTypeNameResolver;
import no.nav.openapi.spec.utils.openapi.RefToClassLookup;
import no.nav.openapi.spec.utils.openapi.RegisteredSubtypesModelConverter;
import no.nav.openapi.spec.utils.openapi.TimeTypesModelConverter;

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

            ModelResolver.enumsAsRef = true;
            final var resolvedRefClassLookup = new RefToClassLookup();
            // --- CRUCIAL: Reset and register model converters BEFORE context building ---
            ModelConverters.reset();

//            var a = TypeNameResolver.std;
            var a =new PrefixStrippingFQNTypeNameResolver("no.nav.foreldrepenger.web.app.", "no.nav.");
            a.setUseFqn(true);
            // Add base ModelResolver with typeNameResolver set on this helper.
            // This must be added first, so that it ends up last in the converter chain
            ModelConverters.getInstance().addConverter(new ModelResolver(this.objectMapper(),  a));

//            ModelConverters.getInstance().addConverter(new NoAnnotationRequiredNullableConverter());
            ModelConverters.getInstance().addConverter(new NotNullAwareModelConverter());

            // EnumVarnamesConverter adds x-enum-varnames for property name on generated enum objects.
//            ModelConverters.getInstance().addConverter(new EnumVarnamesConverter());
            // TimeTypesModelConverter converts Duration to OpenAPI string with format "duration".
            ModelConverters.getInstance().addConverter(new TimeTypesModelConverter());

            Set<Class<?>> registeredSubtypes = allJsonTypeNameClasses();
            ModelConverters.getInstance().addConverter(new RegisteredSubtypesModelConverter(registeredSubtypes));
            ModelConverters.getInstance().addConverter(new JsonSubTypesModelConverter());
            ModelConverters.getInstance().addConverter(new DiscriminatorModelConverter(resolvedRefClassLookup));

//            ModelConverters.getInstance().addConverter(new JsonTypeNameDisctriminatorConverter());

            // --- Build context AFTER converters are registered ---
            var context = new GenericOpenApiContextBuilder<>()
                .openApiConfiguration(oasConfig)
                .buildContext(false);

            context.init();
            context.read();


        } catch (OpenApiConfigurationException e) {
            throw new TekniskException("OPEN-API", e.getMessage(), e);
        }
    }

    protected ObjectMapper objectMapper() {
        final var om = ObjectMapperFactory.createJson();
        // Remove all JsonSubTypes annotations from the objectmapper used in openapi creation. To avoid problem with
        // allOf element being created on subtypes along with oneOf from JsonSubTypesModelConverter
        om.setAnnotationIntrospector(new NoJsonSubTypesAnnotationIntrospector());
        return om;
    }

    public static Set<Class<?>> allJsonTypeNameClasses() {
        // registrer jackson JsonTypeName subtypes basert på rest implementasjoner
        final Collection<Class<?>> restClasses = RestImplementationClasses.getImplementationClasses();

        final Set<Class<?>> scanClasses = new LinkedHashSet<>(restClasses);

        // hack - additional locations to scan (jars uten rest services) - trenger det her p.t. for å bestemme hvilke jars / maven moduler som skal scannes for andre dtoer
//        scanClasses.add(AvklarArbeidsforholdDto.class);
//        scanClasses.add(VurderFaktaOmBeregningDto.class);
//        scanClasses.add(OmsorgspengerSøknadInnsending.class);
//        scanClasses.add(PleiepengerBarnSøknadInnsending.class);
//        scanClasses.add(FrisinnSøknadInnsending.class);

        // avled code location fra klassene
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

//    public ApiConfig() {
//        final var info = new Info()
//            .title("K9 saksbehandling - Saksbehandling av kapittel 9 i folketrygden")
//            .version("2.1")
//            .description("REST grensesnitt for Vedtaksløsningen.");
//        final var server = new Server().url("/k9/sak");
//        final var openapiSetupHelper = new OpenApiSetupHelper(this, info, server);
//        openapiSetupHelper.addResourcePackage("no.nav.k9.");
//        openapiSetupHelper.addResourcePackage("no.nav.k9.sak");
//        openapiSetupHelper.addResourcePackage("no.nav.k9");
//        // The same classes registered as subtypes in object mapper are registered as subtypes in openapi setup helper:
//        openapiSetupHelper.registerSubTypes(allJsonTypeNameClasses());
////        openapiSetupHelper.setTypeNameResolver(new PrefixStrippingFQNTypeNameResolver("no.nav."));
//        try {
//            return openapiSetupHelper.resolveOpenAPI();
//        } catch (OpenApiConfigurationException e) {
//            throw new RuntimeException(e.getMessage(), e);
//        }
//    }

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
