package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.eøs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.validering.ValidKodeverk;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_UTTAK_I_EØS_FOR_ANNENPART)
public class EøsUttakDto extends BekreftetAksjonspunktDto {

    @Valid
    @NotNull
    @Size(max = 200)
    private List<@Valid @NotNull EøsUttakPeriodeDto> perioder;

    public List<EøsUttakPeriodeDto> getPerioder() {
        return perioder;
    }

    public record EøsUttakPeriodeDto(@NotNull LocalDate fom,
                                     @NotNull LocalDate tom,
                                     @NotNull @Min(0) @Max(1000) @Digits(integer = 3, fraction = 2) BigDecimal trekkdager,
                                     @NotNull @ValidKodeverk UttakPeriodeType trekkonto) {
    }
}
