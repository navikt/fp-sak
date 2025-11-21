package no.nav.foreldrepenger.domene.rest.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VurderATogFLiSammeOrganisasjonDto {

    @NotNull
    @Size(max = 100)
    private List<@Valid VurderATogFLiSammeOrganisasjonAndelDto> vurderATogFLiSammeOrganisasjonAndelListe;

    VurderATogFLiSammeOrganisasjonDto() {
        // For Jackson
    }

    public List<VurderATogFLiSammeOrganisasjonAndelDto> getVurderATogFLiSammeOrganisasjonAndelListe() {
        return vurderATogFLiSammeOrganisasjonAndelListe;
    }

    public void setVurderATogFLiSammeOrganisasjonAndelListe(List<VurderATogFLiSammeOrganisasjonAndelDto> vurderATogFLiSammeOrganisasjonAndelListe) {
        this.vurderATogFLiSammeOrganisasjonAndelListe = vurderATogFLiSammeOrganisasjonAndelListe;
    }
}
