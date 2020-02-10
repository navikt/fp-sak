package no.nav.foreldrepenger.mottak.publiserer.producer;

import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.vedtak.feil.Feil;

public class DialogJsonConfig {

    private static final ObjectMapper OM = new ObjectMapper();

    static {
        OM.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        OM.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        OM.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        OM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OM.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
        OM.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OM.registerModule(new JavaTimeModule());
    }

    private DialogJsonConfig() {
        // skjul public constructor
    }

    public static String toJson(Object object, Function<JsonProcessingException, Feil> feilFactory) {
        try {
            return OM.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw feilFactory.apply(e).toException();
        }
    }
}
