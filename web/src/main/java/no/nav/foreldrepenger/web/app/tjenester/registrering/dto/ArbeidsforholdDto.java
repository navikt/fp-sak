package no.nav.foreldrepenger.web.app.tjenester.registrering.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.vedtak.util.InputValideringRegex;

import java.time.LocalDate;

public class ArbeidsforholdDto {

    @Size(max = 100)
    @Pattern(regexp = InputValideringRegex.NAVN)
    private String arbeidsgiver;

    private LocalDate periodeFom;
    private LocalDate periodeTom;

    @Size(min = 1, max = 100)
    @Pattern(regexp = InputValideringRegex.NAVN)
    private String land;

    public String getArbeidsgiver() {
        return arbeidsgiver;
    }

    public void setArbeidsgiver(String arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
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

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }
}
