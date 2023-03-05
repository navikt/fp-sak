package no.nav.foreldrepenger.domene.rest.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

public class FastsettBgKunYtelseDto {

    @Valid
    @Size(min = 1, max = 100)
    private List<FastsattBrukersAndel> andeler;

    private Boolean skalBrukeBesteberegning;

    FastsettBgKunYtelseDto() {
        // For Jackson
    }

    public FastsettBgKunYtelseDto(List<FastsattBrukersAndel> andeler, Boolean skalBrukeBesteberegning) {
        this.andeler = new ArrayList<>(andeler);
        this.skalBrukeBesteberegning = skalBrukeBesteberegning;
    }

    public List<FastsattBrukersAndel> getAndeler() {
        return andeler;
    }

    public void setAndeler(List<FastsattBrukersAndel> andeler) {
        this.andeler = andeler;
    }

    public Boolean getSkalBrukeBesteberegning() {
        return skalBrukeBesteberegning;
    }

    public void setSkalBrukeBesteberegning(Boolean skalBrukeBesteberegning) {
        this.skalBrukeBesteberegning = skalBrukeBesteberegning;
    }
}
