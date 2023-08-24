package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class VurderTidsbegrensetArbeidsforholdDto {

    @Valid
    @NotNull
    @Size(max = 100)
    private List<VurderteArbeidsforholdDto> fastsatteArbeidsforhold;

    VurderTidsbegrensetArbeidsforholdDto() {
        // For Jackson
    }

    public VurderTidsbegrensetArbeidsforholdDto(List<VurderteArbeidsforholdDto> fastsatteArbeidsforhold) {
        this.fastsatteArbeidsforhold = new ArrayList<>(fastsatteArbeidsforhold);
    }

    public List<VurderteArbeidsforholdDto> getFastsatteArbeidsforhold() {
        return fastsatteArbeidsforhold;
    }

    public void setFastsatteArbeidsforhold(List<VurderteArbeidsforholdDto> fastsatteArbeidsforhold) {
        this.fastsatteArbeidsforhold = fastsatteArbeidsforhold;
    }
}
