package no.nav.foreldrepenger.web.app.jackson;

import jakarta.ws.rs.ext.ContextResolver;

import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;
import tools.jackson.databind.json.JsonMapper;

public class JacksonJsonConfig implements ContextResolver<JsonMapper> {

    private static final JsonMapper JSON_MAPPER = createObjectMapper();

    private static synchronized JsonMapper createObjectMapper() {
        var typeNameClasses = RestImplementationClasses.allJsonTypeNameClasses();
        return DefaultJsonMapper.getJsonMapper().rebuild().registerSubtypes(typeNameClasses).build();
    }

    @Override
    public JsonMapper getContext(Class<?> type) {
        return JSON_MAPPER;
    }

}
