package no.nav.foreldrepenger.domene.person.dkif;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DigitalKontaktinfo {

    @JsonProperty("kontaktinfo")
    private Map<String, Kontaktinformasjon> kontaktinfo;

    public Optional<String> getSpraak(String ident) {
        return Optional.ofNullable(kontaktinfo).map(k -> k.get(ident)).map(Kontaktinformasjon::getSpraak);
    }

    private static class Kontaktinformasjon {
        @JsonProperty("spraak")
        private String spraak;

        private String getSpraak() {
            return spraak;
        }
    }

}

