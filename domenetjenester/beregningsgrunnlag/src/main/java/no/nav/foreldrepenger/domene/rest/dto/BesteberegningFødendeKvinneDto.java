package no.nav.foreldrepenger.domene.rest.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public class BesteberegningFødendeKvinneDto {

    @Size(max = 100)
    private List<@Valid BesteberegningFødendeKvinneAndelDto> besteberegningAndelListe;

    @Valid
    private DagpengeAndelLagtTilBesteberegningDto nyDagpengeAndel;

    BesteberegningFødendeKvinneDto() {
        // For Jackson
    }

    public BesteberegningFødendeKvinneDto(List<BesteberegningFødendeKvinneAndelDto> besteberegningAndelListe) {
        this.besteberegningAndelListe = besteberegningAndelListe;
    }

    public BesteberegningFødendeKvinneDto(List<BesteberegningFødendeKvinneAndelDto> besteberegningAndelListe, DagpengeAndelLagtTilBesteberegningDto nyDagpengeAndel) {
        this.nyDagpengeAndel = nyDagpengeAndel;
        this.besteberegningAndelListe = besteberegningAndelListe;
    }

    public List<BesteberegningFødendeKvinneAndelDto> getBesteberegningAndelListe() {
        return besteberegningAndelListe;
    }

    public void setBesteberegningAndelListe(List<BesteberegningFødendeKvinneAndelDto> besteberegningAndelListe) {
        this.besteberegningAndelListe = besteberegningAndelListe;
    }

    public DagpengeAndelLagtTilBesteberegningDto getNyDagpengeAndel() {
        return nyDagpengeAndel;
    }
}
