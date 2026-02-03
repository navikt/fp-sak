package no.nav.foreldrepenger.web.app.jackson;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@Provider
public class JacksonJsonConfig implements ContextResolver<ObjectMapper> {

    private static final JsonMapper JSON_MAPPER = createObjectMapper();

    private static synchronized JsonMapper createObjectMapper() {
        var typeNameClasses = RestImplementationClasses.allJsonTypeNameClasses();
        return DefaultJsonMapper.getJsonMapper().rebuild().registerSubtypes(typeNameClasses).build();
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return JSON_MAPPER;
    }

}
