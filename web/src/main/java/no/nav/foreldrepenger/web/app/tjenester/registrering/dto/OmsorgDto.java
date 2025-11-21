package no.nav.foreldrepenger.web.app.tjenester.registrering.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;

public class OmsorgDto {
    @Min(1)
    @Max(9)
    Integer antallBarn;

    @Size(min = 1, max = 9)
    private List<LocalDate> fødselsdato;

    private LocalDate omsorgsovertakelsesdato;

    private LocalDate ankomstdato;

    private boolean erEktefellesBarn;

    public Integer getAntallBarn() {
        return antallBarn;
    }

    public void setAntallBarn(Integer antallBarn) {
        this.antallBarn = antallBarn;
    }

    @JsonAlias("foedselsDato")
    public List<LocalDate> getFødselsdato() {
        return fødselsdato;
    }

    public void setFødselsdato(List<LocalDate> fødselsdato) {
        this.fødselsdato = fødselsdato;
    }

    public LocalDate getOmsorgsovertakelsesdato() {
        return omsorgsovertakelsesdato;
    }

    public void setOmsorgsovertakelsesdato(LocalDate omsorgsovertakelsesdato) {
        this.omsorgsovertakelsesdato = omsorgsovertakelsesdato;
    }

    public LocalDate getAnkomstdato() {
        return ankomstdato;
    }

    public void setAnkomstdato(LocalDate ankomstdato) {
        this.ankomstdato = ankomstdato;
    }

    public boolean isErEktefellesBarn() {
        return erEktefellesBarn;
    }

    public void setErEktefellesBarn(boolean erEktefellesBarn) {
        this.erEktefellesBarn = erEktefellesBarn;
    }
}
