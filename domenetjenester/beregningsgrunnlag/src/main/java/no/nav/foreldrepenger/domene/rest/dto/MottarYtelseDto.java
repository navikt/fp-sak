package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;


public class MottarYtelseDto {

    private Boolean frilansMottarYtelse;
    @Valid
    @Size(max = 100)
    private List<ArbeidstakerandelUtenIMMottarYtelseDto> arbeidstakerUtenIMMottarYtelse;

    MottarYtelseDto() {
        // For Jackson
    }

    public MottarYtelseDto(Boolean frilansMottarYtelse, List<ArbeidstakerandelUtenIMMottarYtelseDto> arbeidstakerUtenIMMottarYtelse) {
        this.frilansMottarYtelse = frilansMottarYtelse;
        this.arbeidstakerUtenIMMottarYtelse = arbeidstakerUtenIMMottarYtelse;
    }


    public Boolean getFrilansMottarYtelse() {
        return frilansMottarYtelse;
    }

    public List<ArbeidstakerandelUtenIMMottarYtelseDto> getArbeidstakerUtenIMMottarYtelse() {
        return arbeidstakerUtenIMMottarYtelse;
    }

    public void setFrilansMottarYtelse(Boolean frilansMottarYtelse) {
        this.frilansMottarYtelse = frilansMottarYtelse;
    }

    public void setArbeidstakerUtenIMMottarYtelse(List<ArbeidstakerandelUtenIMMottarYtelseDto> arbeidstakerUtenIMMottarYtelse) {
        this.arbeidstakerUtenIMMottarYtelse = arbeidstakerUtenIMMottarYtelse;
    }
}
