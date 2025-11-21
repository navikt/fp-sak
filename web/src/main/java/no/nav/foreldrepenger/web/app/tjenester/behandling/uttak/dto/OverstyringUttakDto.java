package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_UTTAKPERIODER_KODE)
public class OverstyringUttakDto extends OverstyringAksjonspunktDto {


    @JsonProperty("perioder")
    @NotNull
    @Size(min = 1, max = 1500)
    private List<@Valid UttakResultatPeriodeLagreDto> perioder;

    OverstyringUttakDto() {
        // jackson
    }

    public OverstyringUttakDto(List<UttakResultatPeriodeLagreDto> perioder) {
        this.perioder = perioder;
    }

    public List<UttakResultatPeriodeLagreDto> getPerioder() {
        return perioder;
    }

    @JsonIgnore
    @Override
    public boolean getErVilkarOk() {
        //Brukes ikke
        return false;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        //Brukes ikke
        return null;
    }

}
