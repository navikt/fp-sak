package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.SJEKK_TERMINBEKREFTELSE_KODE)
public class SjekkTerminbekreftelseDto extends BekreftetAksjonspunktDto {

    @NotNull
    private LocalDate utstedtdato;

    @NotNull
    private LocalDate termindato;

    @NotNull
    @Min(1)
    @Max(9)
    private int antallBarn;

    SjekkTerminbekreftelseDto() {
        // For Jackson
    }

    public SjekkTerminbekreftelseDto(String begrunnelse, LocalDate termindato, LocalDate utstedtdato, int antallBarn) {
        super(begrunnelse);
        this.termindato = termindato;
        this.utstedtdato = utstedtdato;
        this.antallBarn = antallBarn;
    }

    public LocalDate getUtstedtdato() {
        return utstedtdato;
    }

    public LocalDate getTermindato() {
        return termindato;
    }

    public int getAntallBarn() {
        return antallBarn;
    }
}
