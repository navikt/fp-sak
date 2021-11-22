package no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

public class ArbeidOgInntektsmeldingDto {
    @JsonProperty(value = "inntektsmeldinger")
    @NotNull
    @Valid
    private List<InntektsmeldingDto> inntektsmeldinger;

    @JsonProperty(value = "arbeidsforhold")
    @NotNull
    @Valid
    private List<ArbeidsforholdDto> arbeidsforhold;

    @JsonProperty(value = "inntekter")
    @NotNull
    @Valid
    private List<InntektDto> inntekter;


    public ArbeidOgInntektsmeldingDto(List<InntektsmeldingDto> inntektsmeldinger,
                                      List<ArbeidsforholdDto> arbeidsforhold,
                                      List<InntektDto> inntekter) {
        this.inntektsmeldinger = inntektsmeldinger;
        this.arbeidsforhold = arbeidsforhold;
        this.inntekter = inntekter;
    }

    public List<InntektsmeldingDto> getInntektsmeldinger() {
        return inntektsmeldinger;
    }

    public List<ArbeidsforholdDto> getArbeidsforhold() {
        return arbeidsforhold;
    }

    public List<InntektDto> getInntekter() {
        return inntekter;
    }
}
