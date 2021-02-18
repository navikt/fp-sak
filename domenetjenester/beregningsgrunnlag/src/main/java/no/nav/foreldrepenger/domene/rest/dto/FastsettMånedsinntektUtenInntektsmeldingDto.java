package no.nav.foreldrepenger.domene.rest.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

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
