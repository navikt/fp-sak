package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class FastsettMånedsinntektUtenInntektsmeldingDto {

    @Valid
    @NotNull
    @Size(max = 100)
    private List<FastsettMånedsinntektUtenInntektsmeldingAndelDto> andelListe;

    public List<FastsettMånedsinntektUtenInntektsmeldingAndelDto> getAndelListe() {
        return andelListe;
    }

    public void setAndelListe(List<FastsettMånedsinntektUtenInntektsmeldingAndelDto> andelListe) {
        this.andelListe = andelListe;
    }
}
