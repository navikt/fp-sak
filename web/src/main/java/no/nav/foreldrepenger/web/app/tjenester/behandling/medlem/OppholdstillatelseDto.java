package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;

import java.time.LocalDate;

public class OppholdstillatelseDto {
    private LocalDate fom;
    private LocalDate tom;
    private OppholdstillatelseType oppholdstillatelseType;

    public OppholdstillatelseDto() {
        // trengs for deserialisering av JSON
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    public void setOppholdstillatelseType(OppholdstillatelseType oppholdstillatelseType) {
        this.oppholdstillatelseType = oppholdstillatelseType;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public OppholdstillatelseType getOppholdstillatelseType() {
        return oppholdstillatelseType;
    }
}
