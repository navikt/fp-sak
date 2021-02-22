package no.nav.foreldrepenger.domene.rest.dto;

import java.time.LocalDate;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class FastsettBeregningsgrunnlagPeriodeDto {

    @Valid
    @Size(max = 100)
    private List<FastsettBeregningsgrunnlagAndelDto> andeler;
    @NotNull
    private LocalDate fom;
    private LocalDate tom;



    FastsettBeregningsgrunnlagPeriodeDto() { // NOSONAR
        // Jackson
    }

    public FastsettBeregningsgrunnlagPeriodeDto(List<FastsettBeregningsgrunnlagAndelDto> andeler, LocalDate fom, LocalDate tom) { // NOSONAR
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
