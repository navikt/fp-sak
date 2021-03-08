package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.kafka;

import java.io.IOException;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.vedtak.exception.TekniskException;

public class JsonObjectMapper {

    private static final ObjectMapper OM;
    static {
        OM = new ObjectMapper();
        OM.registerModule(new JavaTimeModule());
        OM.registerModule(new Jdk8Module());
        OM.setVisibility(PropertyAccessor.GETTER, Visibility.NONE);
        OM.setVisibility(PropertyAccessor.SETTER, Visibility.NONE);
        OM.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        OM.setVisibility(PropertyAccessor.CREATOR, Visibility.ANY);
        OM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OM.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static String toJson(Object object, Function<JsonProcessingException, TekniskException> feilFactory) {
        try {
            return OM.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw feilFactory.apply(e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return OM.readerFor(clazz).readValue(json);
        } catch (IOException e) {
            throw new IllegalArgumentException("Feil i deserialisering");
        }
    }
}
