package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.eøs;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_UTTAK_I_EØS_FOR_ANNENPART_KODE)
public class EøsUttakDto extends BekreftetAksjonspunktDto {

    @Valid
    @NotNull
    @Size(max = 200)
    private List<@Valid @NotNull EøsUttakPeriodeDto> perioder;

    public List<EøsUttakPeriodeDto> getPerioder() {
        return perioder;
    }
}
