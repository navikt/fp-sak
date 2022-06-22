package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ArbeidsforholdDto {

    // NOSONAR
    private String id;
    // For ny visning i GUI (orgnr eller aktørId)
    private String arbeidsgiverReferanse;
    private String arbeidsforholdId;
    private String eksternArbeidsforholdId;
    private BigDecimal stillingsprosent;
    private LocalDate fomDato;
    private LocalDate tomDato;

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
    }

    public void setArbeidsgiverReferanse(String arbeidsgiverReferanse) {
        this.arbeidsgiverReferanse = arbeidsgiverReferanse;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public void setFomDato(LocalDate fomDato) {
        this.fomDato = fomDato;
    }

    public LocalDate getTomDato() {
        return tomDato;
    }

    public void setTomDato(LocalDate tomDato) {
        this.tomDato = tomDato;
    }

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public void setArbeidsforholdId(String arbeidsforholdId) {
        this.arbeidsforholdId = arbeidsforholdId;
    }

    public String getEksternArbeidsforholdId() {
        return eksternArbeidsforholdId;
    }

    public void setEksternArbeidsforholdId(String eksternArbeidsforholdId) {
        this.eksternArbeidsforholdId = eksternArbeidsforholdId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public void setStillingsprosent(BigDecimal stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
    }


}
