package no.nav.foreldrepenger.historikk.kafka.json;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.foreldrepenger.historikk.kafka.feil.HistorikkFeil;

public final class SerialiseringUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new Jdk8Module());
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.registerModule(new SimpleModule());
    }

    public static <T> T deserialiser(String melding, Class<T> klassetype) {
        try {
            return OBJECT_MAPPER.readValue(melding, klassetype);
        } catch (IOException e) {
            throw HistorikkFeil.FACTORY.klarteIkkeDeserialisere(klassetype.getName(), e).toException();
        }
    }
    
    private SerialiseringUtil() {
        // util class
    }

}
