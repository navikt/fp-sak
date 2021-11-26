package no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ArbeidsforholdDto {
    @JsonProperty(value = "arbeidsgiverIdent")
    @NotNull
    @Valid
    private String arbeidsgiverIdent;

    @JsonProperty(value = "internArbeidsforholdId")
    @NotNull
    @Valid
    private String internArbeidsforholdId;

    @JsonProperty(value = "eksternArbeidsforholdId")
    @NotNull
    @Valid
    private String eksternArbeidsforholdId;

    @JsonProperty(value = "fom")
    @NotNull
    @Valid
    private LocalDate fom;

    @JsonProperty(value = "tom")
    @NotNull
    @Valid
    private LocalDate tom;

    @JsonProperty(value = "stillingsprosent")
    @NotNull
    @Valid
    private BigDecimal stillingsprosent;

    public ArbeidsforholdDto(String arbeidsgiverIdent,
                             String internArbeidsforholdId,
                             String eksternArbeidsforholdId,
                             LocalDate fom,
                             LocalDate tom,
                             BigDecimal stillingsprosent) {
        this.arbeidsgiverIdent = arbeidsgiverIdent;
        this.internArbeidsforholdId = internArbeidsforholdId;
        this.eksternArbeidsforholdId = eksternArbeidsforholdId;
        this.fom = fom;
        this.tom = tom;
        this.stillingsprosent = stillingsprosent;
    }

    public String getArbeidsgiverIdent() {
        return arbeidsgiverIdent;
    }

    public String getInternArbeidsforholdId() {
        return internArbeidsforholdId;
    }

    public String getEksternArbeidsforholdId() {
        return eksternArbeidsforholdId;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }
}
