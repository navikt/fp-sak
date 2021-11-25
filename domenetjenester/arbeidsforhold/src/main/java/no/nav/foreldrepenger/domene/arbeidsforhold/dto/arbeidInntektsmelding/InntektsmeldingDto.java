package no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class InntektsmeldingDto {
    @JsonProperty(value = "inntektPrMnd")
    @NotNull
    @Valid
    private BigDecimal inntektPrMnd;

    @JsonProperty(value = "refusjonPrMnd")
    @Valid
    private BigDecimal refusjonPrMnd;

    @JsonProperty(value = "arbeidsgiverIdent")
    @NotNull
    @Valid
    private String arbeidsgiverIdent;

    @JsonProperty(value = "eksternArbeidsforholdId")
    @Valid
    private String eksternArbeidsforholdId;

    @JsonProperty(value = "internArbeidsforholdId")
    @Valid
    private String internArbeidsforholdId;

    @JsonProperty(value = "kontaktpersonNavn")
    @Valid
    private String kontaktpersonNavn;

    @JsonProperty(value = "kontaktpersonNummer")
    @Valid
    private String kontaktpersonNummer;

    @JsonProperty(value = "motattDato")
    @NotNull
    @Valid
    private LocalDate motattDato;

    public InntektsmeldingDto(BigDecimal inntektPrMnd,
                              BigDecimal refusjonPrMnd,
                              String arbeidsgiverIdent,
                              String eksternArbeidsforholdId,
                              String internArbeidsforholdId,
                              String kontaktpersonNavn,
                              String kontaktpersonNummer,
                              LocalDate motattDato) {
        this.inntektPrMnd = inntektPrMnd;
        this.refusjonPrMnd = refusjonPrMnd;
        this.arbeidsgiverIdent = arbeidsgiverIdent;
        this.eksternArbeidsforholdId = eksternArbeidsforholdId;
        this.kontaktpersonNavn = kontaktpersonNavn;
        this.kontaktpersonNummer = kontaktpersonNummer;
        this.motattDato = motattDato;
        this.internArbeidsforholdId = internArbeidsforholdId;
    }

    public BigDecimal getInntektPrMnd() {
        return inntektPrMnd;
    }

    public BigDecimal getRefusjonPrMnd() {
        return refusjonPrMnd;
    }

    public String getArbeidsgiverIdent() {
        return arbeidsgiverIdent;
    }

    public String getEksternArbeidsforholdId() {
        return eksternArbeidsforholdId;
    }

    public String getKontaktpersonNavn() {
        return kontaktpersonNavn;
    }

    public String getKontaktpersonNummer() {
        return kontaktpersonNummer;
    }

    public String getInternArbeidsforholdId() {
        return internArbeidsforholdId;
    }

    public LocalDate getMotattDato() {
        return motattDato;
    }
}
