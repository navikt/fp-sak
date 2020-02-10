package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto;

import java.time.LocalDate;

public class TotrinnskontrollAktivitetDto {

    private String aktivitetType;
    private Boolean erEndring;
    private String arbeidsgiverNavn;
    private String orgnr;
    private boolean godkjent;
    private LocalDate privatpersonFødselsdato;

    public TotrinnskontrollAktivitetDto() {
        //Tom
    }

    public String getAktivitetType() {
        return aktivitetType;
    }

    public Boolean getErEndring() {
        return erEndring;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public LocalDate getPrivatpersonFødselsdato() {
        return privatpersonFødselsdato;
    }

    public boolean isGodkjent() {
        return godkjent;
    }

    public void setAktivitetType(String aktivitetType) {
        this.aktivitetType = aktivitetType;
    }

    public void setErEndring(Boolean erEndring) {
        this.erEndring = erEndring;
    }

    public void setArbeidsgiverNavn(String arbeidsgiverNavn) {
        this.arbeidsgiverNavn = arbeidsgiverNavn;
    }

    public void setOrgnr(String orgnr) {
        this.orgnr = orgnr;
    }

    public void setGodkjent(boolean godkjent) {
        this.godkjent = godkjent;
    }

    public void setPrivatpersonFødselsdato(LocalDate privatpersonFødselsdato) {
        this.privatpersonFødselsdato = privatpersonFødselsdato;
    }
}
