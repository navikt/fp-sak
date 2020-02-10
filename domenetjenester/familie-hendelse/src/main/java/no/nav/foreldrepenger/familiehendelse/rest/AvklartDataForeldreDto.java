package no.nav.foreldrepenger.familiehendelse.rest;

import java.time.LocalDate;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.domene.typer.AktørId;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AvklartDataForeldreDto {

    @JsonProperty("aktorId")
    @Valid
    private AktørId aktørId;

    @JsonProperty("dodsdato")
    private LocalDate dødsdato;

    public AktørId getAktorId() {
        return aktørId;
    }

    public void setAktorId(AktørId aktorId) {
        this.aktørId = aktorId;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    public void setDødsdato(LocalDate dødsdato) {
        this.dødsdato = dødsdato;
    }
}
