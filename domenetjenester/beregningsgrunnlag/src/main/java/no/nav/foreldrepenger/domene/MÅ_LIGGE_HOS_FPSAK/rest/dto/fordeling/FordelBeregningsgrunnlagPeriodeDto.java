package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.fordeling;

import java.time.LocalDate;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class FordelBeregningsgrunnlagPeriodeDto {

    @Valid
    @Size(max = 100)
    private List<FordelBeregningsgrunnlagAndelDto> andeler;
    @NotNull
    private LocalDate fom;
    private LocalDate tom;



    FordelBeregningsgrunnlagPeriodeDto() { // NOSONAR
        // Jackson
    }

    public FordelBeregningsgrunnlagPeriodeDto(List<FordelBeregningsgrunnlagAndelDto> andeler, LocalDate fom, LocalDate tom) { // NOSONAR
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

    public List<FordelBeregningsgrunnlagAndelDto> getAndeler() {
        return andeler;
    }

}
