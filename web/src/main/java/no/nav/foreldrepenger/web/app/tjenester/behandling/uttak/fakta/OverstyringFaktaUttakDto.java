package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_FAKTA_UTTAK_KODE)
public class OverstyringFaktaUttakDto extends OverstyringAksjonspunktDto {

    @Valid
    @NotNull
    @Size(min = 1, max = 200)
    private List<FaktaUttakPeriodeDto> perioder;

    OverstyringFaktaUttakDto() {
        // jackson
    }

    public List<FaktaUttakPeriodeDto> getPerioder() {
        return perioder;
    }

    public void setPerioder(List<FaktaUttakPeriodeDto> perioder) {
        this.perioder = perioder;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        return null;
    }

    @JsonIgnore
    @Override
    public boolean getErVilkarOk() {
        return false;
    }
}
