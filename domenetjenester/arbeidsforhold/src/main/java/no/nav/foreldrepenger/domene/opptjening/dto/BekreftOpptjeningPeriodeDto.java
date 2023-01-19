package no.nav.foreldrepenger.domene.opptjening.dto;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;

public class BekreftOpptjeningPeriodeDto {
    private OpptjeningAktivitetType aktivitetType;
    private LocalDate opptjeningFom;
    private LocalDate opptjeningTom;
    private String arbeidsgiverNavn;
    private String arbeidsgiverReferanse;
    private String arbeidsforholdRef;
    private Boolean erGodkjent;
    private String begrunnelse;

    public BekreftOpptjeningPeriodeDto() {
    }

    public OpptjeningAktivitetType getAktivitetType() {
        return aktivitetType;
    }

    public void setAktivitetType(OpptjeningAktivitetType aktivitetType) {
        this.aktivitetType = aktivitetType;
    }

    public LocalDate getOpptjeningFom() {
        return opptjeningFom;
    }

    public void setOpptjeningFom(LocalDate opptjeningFom) {
        this.opptjeningFom = opptjeningFom;
    }

    public LocalDate getOpptjeningTom() {
        return opptjeningTom;
    }

    public void setOpptjeningTom(LocalDate opptjeningTom) {
        this.opptjeningTom = opptjeningTom;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

    public void setArbeidsgiverNavn(String arbeidsgiverNavn) {
        this.arbeidsgiverNavn = arbeidsgiverNavn;
    }

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
    }

    public void setArbeidsgiverReferanse(String arbeidsgiverReferanse) {
        this.arbeidsgiverReferanse = arbeidsgiverReferanse;
    }

    public Boolean getErGodkjent() {
        return erGodkjent;
    }

    public void setErGodkjent(Boolean erGodkjent) {
        this.erGodkjent = erGodkjent;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public String getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public void setArbeidsforholdRef(String arbeidsforholdRef) {
        this.arbeidsforholdRef = arbeidsforholdRef;
    }

    @Override
    public String toString() {
        return "BekreftOpptjeningPeriodeDto{" +
            "aktivitetType=" + aktivitetType +
            ", opptjeningFom=" + opptjeningFom +
            ", opptjeningTom=" + opptjeningTom +
            ", arbeidsforholdRef='" + arbeidsforholdRef + '\'' +
            ", erGodkjent=" + erGodkjent +
            ", begrunnelse='" + begrunnelse + '\'' +
            '}';
    }
}
