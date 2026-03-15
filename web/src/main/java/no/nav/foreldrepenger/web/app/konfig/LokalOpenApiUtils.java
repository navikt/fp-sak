package no.nav.foreldrepenger.web.app.konfig;

import java.util.Set;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;
import no.nav.openapi.spec.utils.openapi.DiscriminatorModelConverter;
import no.nav.openapi.spec.utils.openapi.EnumVarnamesConverter;
import no.nav.openapi.spec.utils.openapi.JsonSubTypesModelConverter;
import no.nav.openapi.spec.utils.openapi.NoJsonSubTypesAnnotationIntrospector;
import no.nav.openapi.spec.utils.openapi.PrefixStrippingFQNTypeNameResolver;
import no.nav.openapi.spec.utils.openapi.RefToClassLookup;
import no.nav.openapi.spec.utils.openapi.RegisteredSubtypesModelConverter;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

public class LokalOpenApiUtils {

    private LokalOpenApiUtils() {
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

        Set<Class<?>> registeredSubtypes = RestImplementationClasses.allJsonTypeNameClasses();
        ModelConverters.getInstance().addConverter(new RegisteredSubtypesModelConverter(registeredSubtypes));
        ModelConverters.getInstance().addConverter(new JsonSubTypesModelConverter());
        ModelConverters.getInstance().addConverter(new DiscriminatorModelConverter(new RefToClassLookup()));
        ModelConverters.getInstance().addConverter(new EnumVarnamesConverter());
    }

    private static JsonMapper lagObjectMapperUtenJsonSubTypeAnnotasjoner() {
        return DefaultJsonMapper.getJsonMapper().rebuild()
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
