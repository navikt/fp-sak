package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import no.nav.foreldrepenger.domene.iay.modell.Gradering;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GraderingPeriodeDto {
    private LocalDate fom;
    private LocalDate tom;
    private BigDecimal arbeidsprosent;

    public GraderingPeriodeDto(Gradering gradering) {
        this.fom = gradering.getPeriode().getFomDato();
        this.tom = gradering.getPeriode().getTomDato();
        this.arbeidsprosent = gradering.getArbeidstidProsent().getVerdi();
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public BigDecimal getArbeidsprosent() {
        return arbeidsprosent;
    }
}
