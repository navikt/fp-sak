package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.util.Objects;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.domene.typer.Saksnummer;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
public class SaksnummerDto {

    @JsonProperty("saksnummer")
    @NotNull
    @Digits(integer = 18, fraction = 0)
    private String saksnummer;

    public SaksnummerDto() {
        //For Jackson
    }

    public SaksnummerDto(Long saksnummer) {
        Objects.requireNonNull(saksnummer, "saksnummer");
        this.saksnummer = saksnummer.toString();
    }

    public SaksnummerDto(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public SaksnummerDto(Saksnummer saksnummer) {
        this.saksnummer = saksnummer.getVerdi();
    }


    public String getVerdi() {
        return saksnummer;
    }

    public Long getVerdiSomLong() {
        return Long.parseLong(saksnummer);
    }

    @Override
    public String toString() {
        return "SaksnummerDto{" +
            "saksnummer='" + saksnummer + '\'' +
            '}';
    }

}
