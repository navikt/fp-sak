package no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

public class InntektDto {
    @JsonProperty(value = "arbeidsgiverIdent")
    @NotNull
    @Valid
    private String arbeidsgiverIdent;

    @JsonProperty(value = "inntekter")
    @NotNull
    @Valid
    private List<InntektspostDto> inntekter;

    public InntektDto(String arbeidsgiverIdent, List<InntektspostDto> inntekter) {
        this.arbeidsgiverIdent = arbeidsgiverIdent;
        this.inntekter = inntekter;
    }

    public String getArbeidsgiverIdent() {
        return arbeidsgiverIdent;
    }

    public List<InntektspostDto> getInntekter() {
        return inntekter;
    }
}
