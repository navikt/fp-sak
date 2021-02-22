package no.nav.foreldrepenger.domene.rest.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class VurderATogFLiSammeOrganisasjonDto {

    @Valid
    @NotNull
    @Size(max = 100)
    private List<VurderATogFLiSammeOrganisasjonAndelDto> vurderATogFLiSammeOrganisasjonAndelListe;

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
