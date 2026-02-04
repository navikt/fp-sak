package no.nav.foreldrepenger.domene.json;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

public final class StandardJsonConfig {

    // Dette gjør det litt enklere å bytte ut denne med Jackson3
    private static final JsonMapper OM = DefaultJsonMapper.getJsonMapper();

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

