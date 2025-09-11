package no.nav.foreldrepenger.familiehendelse.rest;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

public class AvklartBarnDto {
    @NotNull private LocalDate fodselsdato;
    private LocalDate dodsdato;

    public AvklartBarnDto(LocalDate fodselsdato, LocalDate dodsdato) {
        this.fodselsdato = fodselsdato;
        this.dodsdato = dodsdato;
    }

    @JsonProperty("fodselsdato")
    public LocalDate getFodselsdato() {
        return fodselsdato;
    }

    public void setFodselsdato(LocalDate fodselsdato) {
        this.fodselsdato = fodselsdato;
    }

    @JsonProperty("dodsdato")
    public LocalDate getDodsdato() {
        return dodsdato;
    }

    public void setDodsdato(LocalDate dodsdato) {
        this.dodsdato = dodsdato;
    }
}
