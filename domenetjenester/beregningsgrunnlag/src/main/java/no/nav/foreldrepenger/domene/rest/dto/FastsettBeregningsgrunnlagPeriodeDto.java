package no.nav.foreldrepenger.domene.rest.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FastsettBeregningsgrunnlagPeriodeDto {

    @Size(max = 100)
    private List<@Valid FastsettBeregningsgrunnlagAndelDto> andeler;
    @NotNull
    private LocalDate fom;
    private LocalDate tom;



    FastsettBeregningsgrunnlagPeriodeDto() {
        // Jackson
    }

    public FastsettBeregningsgrunnlagPeriodeDto(List<FastsettBeregningsgrunnlagAndelDto> andeler, LocalDate fom, LocalDate tom) {
        this.andeler = andeler;
        this.fom = fom;
        this.tom = tom;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public List<FastsettBeregningsgrunnlagAndelDto> getAndeler() {
        return andeler;
    }

}
