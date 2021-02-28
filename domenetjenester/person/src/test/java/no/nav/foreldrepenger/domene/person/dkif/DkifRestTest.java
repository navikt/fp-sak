package no.nav.foreldrepenger.domene.person.dkif;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


public class DkifRestTest {

    private static ObjectMapper mapper;

    private static final String json = """
        {
          "kontaktinfo": {
            "12345678901": {
              "personident": "12345678901",
              "kanVarsles": false,
              "reservert": false,
              "epostadresse": "noreply@nav.no",
              "mobiltelefonnummer": "11111111",
              "spraak": "nb"
            }
          }
        }
        """;

    static  {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    public void roundtrip_organisasjon() throws IOException {

        var k = fromJson(json, DigitalKontaktinfo.class);

        assertThat(k.getSpraak("12345678901")).isPresent();
        assertThat(k.getSpraak("12345678901")).hasValueSatisfying(v -> assertThat(v).isEqualTo("nb"));
        assertThat(k.getSpraak("12345678902")).isNotPresent();
    }

    @Test
    public void mapping_jurdisk_enhet() throws IOException {
        // Arrange
        String ny = """
            {
              "kontaktinfo": {
                "12345678901": {
                  "personident": "12345678901",
                  "kanVarsles": false,
                  "reservert": false,
                  "epostadresse": "noreply@nav.no",
                  "mobiltelefonnummer": "11111111",
                  "spraak": "nb"
                },
                "12345678902": {
                  "personident": "12345678902",
                  "kanVarsles": false,
                  "reservert": false,
                  "epostadresse": "noreply@nav.no",
                  "mobiltelefonnummer": "11111111",
                  "spraak": "nn"
                }
              }
            }
            """;

        var k = fromJson(ny, DigitalKontaktinfo.class);
        assertThat(k.getSpraak("12345678901")).isPresent();
        assertThat(k.getSpraak("12345678902")).hasValueSatisfying(v -> assertThat(v).isEqualTo("nn"));
        assertThat(k.getSpraak("12345678903")).isNotPresent();
    }

    private static <T> T fromJson(String json, Class<T> clazz) throws IOException {
        return mapper.readerFor(clazz).readValue(json);
    }
}
