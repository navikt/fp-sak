package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.DokumentasjonPeriodeEntitet;

public class UttakDokumentasjonDto {
    @NotNull
    private LocalDate fom;

    @NotNull
    private LocalDate tom;

    UttakDokumentasjonDto() { // NOSONAR
        //for jackson
    }

    public UttakDokumentasjonDto(DokumentasjonPeriodeEntitet<?> dokumentasjon) {
        this.fom = dokumentasjon.getPeriode().getFomDato();
        this.tom = dokumentasjon.getPeriode().getTomDato();
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }
}
