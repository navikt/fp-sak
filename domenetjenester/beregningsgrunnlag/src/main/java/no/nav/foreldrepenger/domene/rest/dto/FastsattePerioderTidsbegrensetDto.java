package no.nav.foreldrepenger.domene.rest.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class FastsattePerioderTidsbegrensetDto {

    @NotNull
    private LocalDate periodeFom;
    private LocalDate periodeTom;

    private List<@Valid FastsatteAndelerTidsbegrensetDto> fastsatteTidsbegrensedeAndeler;

    FastsattePerioderTidsbegrensetDto() {
        // Jackson
    }

    public FastsattePerioderTidsbegrensetDto(LocalDate periodeFom,
                                             LocalDate periodeTom,
                                             List<FastsatteAndelerTidsbegrensetDto> fastsatteTidsbegrensedeAndeler) {
        this.periodeFom = periodeFom;
        this.periodeTom = periodeTom;
        this.fastsatteTidsbegrensedeAndeler = fastsatteTidsbegrensedeAndeler;
    }

    public LocalDate getPeriodeFom() {
        return periodeFom;
    }

    public LocalDate getPeriodeTom() {
        return periodeTom;
    }

    public List<FastsatteAndelerTidsbegrensetDto> getFastsatteTidsbegrensedeAndeler() {
        return fastsatteTidsbegrensedeAndeler;
    }
}
