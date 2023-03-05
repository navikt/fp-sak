package no.nav.foreldrepenger.domene.rest.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

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
