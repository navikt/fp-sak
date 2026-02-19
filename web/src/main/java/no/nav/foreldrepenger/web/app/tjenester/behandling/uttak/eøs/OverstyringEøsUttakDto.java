package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.eøs;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_UTTAK_I_EØS_FOR_ANNENPART_KODE)
public class OverstyringEøsUttakDto extends OverstyringAksjonspunktDto {

    @NotNull
    @Size(max = 200)
    private List<@Valid @NotNull EøsUttakPeriodeDto> perioder;

    public List<EøsUttakPeriodeDto> getPerioder() {
        return perioder;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        return null;
    }

    @JsonIgnore
    @Override
    public boolean getErVilkårOk() {
        return false;
    }
}
