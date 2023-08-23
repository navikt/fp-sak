package no.nav.foreldrepenger.web.app.tjenester.registrering.dto;

import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.validering.ValidKodeverk;

import java.time.LocalDate;

public class OverføringsperiodeDto {

    @NotNull
    private LocalDate periodeFom;

    @NotNull
    private LocalDate periodeTom;

    @NotNull
    @ValidKodeverk
    private OverføringÅrsak overforingArsak;

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

    public OverføringÅrsak getOverforingArsak() {
        return overforingArsak;
    }

    public void setOverforingArsak(OverføringÅrsak overforingArsak) {
        this.overforingArsak = overforingArsak;
    }
}
