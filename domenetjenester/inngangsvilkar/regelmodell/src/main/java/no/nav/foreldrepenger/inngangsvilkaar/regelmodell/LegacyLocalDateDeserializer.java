package no.nav.foreldrepenger.inngangsvilkaar.regelmodell;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

public class LegacyLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        try {
            var year = node.get("year").intValue();
            var month = Month.valueOf(node.get("month").asText());
            var day = node.get("dayOfMonth").intValue();
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            throw JsonMappingException.from(parser, node.toString(), e);
        }
    }
}
