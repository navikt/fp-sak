package no.nav.foreldrepenger.familiehendelse.rest;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.validation.constraints.NotNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AvklartDataFodselDto.class),
    @JsonSubTypes.Type(value = AvklartDataAdopsjonDto.class),
    @JsonSubTypes.Type(value = AvklartDataOmsorgDto.class)
})
public abstract class FamiliehendelseDto {

    @NotNull private SøknadType soknadType;
    @NotNull private LocalDate skjæringstidspunkt;

    public FamiliehendelseDto() {
    }

    public FamiliehendelseDto(SøknadType søknadType) {
        this.soknadType = søknadType;
    }

    public boolean erSoknadsType(SøknadType søknadType) {
        return søknadType.equals(this.soknadType);
    }

    @JsonProperty("soknadType")
    public SøknadType getSoknadType() {
        return soknadType;
    }

    @JsonProperty("skjaringstidspunkt")
    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public void setSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }

}
