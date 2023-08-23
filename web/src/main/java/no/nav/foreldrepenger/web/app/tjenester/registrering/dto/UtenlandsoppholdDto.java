package no.nav.foreldrepenger.web.app.tjenester.registrering.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.vedtak.util.InputValideringRegex;

import java.time.LocalDate;

public class UtenlandsoppholdDto {

    @NotNull
    @Size(min = 1, max = 100)
    @Pattern(regexp = InputValideringRegex.NAVN)
    private String land;

    @NotNull
    private LocalDate periodeFom;

    @NotNull
    private LocalDate periodeTom;

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }

    public LocalDate getPeriodeFom() {
        return periodeFom;
    }

    public void setPeriodeFom(LocalDate periodeFom) {
        this.periodeFom = periodeFom;
    }

    public LocalDate getPeriodeTom() {
        return periodeTom;
    }

    public void setPeriodeTom(LocalDate periodeTom) {
        this.periodeTom = periodeTom;
    }

}
