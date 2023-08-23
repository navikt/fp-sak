package no.nav.foreldrepenger.domene.opptjening.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

import java.time.LocalDate;

public class AvklarOpptjeningAktivitetDto {

    @ValidKodeverk
    private OpptjeningAktivitetType aktivitetType;

    private LocalDate opptjeningFom;

    private LocalDate opptjeningTom;

    @Pattern(regexp = "\\d{7}|\\d{9}|\\d{13}")
    private String arbeidsgiverReferanse;

    @Size(max = 100)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String arbeidsforholdRef;

    private Boolean erGodkjent;

    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    public AvklarOpptjeningAktivitetDto() {
        // trengs for deserialisering av JSON
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
}
