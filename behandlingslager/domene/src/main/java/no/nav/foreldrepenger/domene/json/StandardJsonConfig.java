package no.nav.foreldrepenger.domene.json;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.vedtak.exception.TekniskException;

public final class StandardJsonConfig {

    private static final ObjectMapper OM = new ObjectMapper();

    static {
        OM.registerModule(new Jdk8Module());
        OM.registerModule(new JavaTimeModule());
        OM.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OM.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        OM.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        OM.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        OM.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        OM.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        OM.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
    }

    private StandardJsonConfig() {
        // skjul public constructor
    }

    public static String toJson(Object object) {
        try {
            Writer jsonWriter = new StringWriter();
            OM.writerWithDefaultPrettyPrinter().writeValue(jsonWriter, object);
            jsonWriter.flush();
            return jsonWriter.toString();
        } catch (IOException e) {
            throw new TekniskException("FP-713329", "Fikk IO exception ved serialisering til JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return OM.readerFor(clazz).readValue(json);
        } catch (IOException e) {
            throw deserialiseringException(e);
        }
    }

    public static <T> T fromJson(URL json, Class<T> clazz) {
        try {
            return OM.readerFor(clazz).readValue(json.openStream());
        } catch (IOException e) {
            throw deserialiseringException(e);
        }
    }

    public static JsonNode fromJsonAsTree(String json) {
        try {
            return OM.readTree(json);
        } catch (IOException e) {
            throw deserialiseringException(e);
        }
    }

    private static TekniskException deserialiseringException(IOException e) {
        return new TekniskException("FP-713328", "Fikk IO exception ved deserialisering av JSON", e);
    }
}

