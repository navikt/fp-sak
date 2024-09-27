package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;

import java.time.LocalDate;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_FORUTGÅENDE_MEDLEMSKAPSVILKÅR_KODE)
public class OverstyringForutgåendeMedlemskapsvilkårDto extends OverstyringAksjonspunktDto {


    @JsonProperty("avslagskode")
    @Size(min = 4, max = 4)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String avslagskode;

    @JsonProperty("erVilkarOk")
    private boolean erVilkarOk;

    @JsonProperty("medlemFom")
    private LocalDate medlemFom;

    @SuppressWarnings("unused")
    private OverstyringForutgåendeMedlemskapsvilkårDto() {
        super();
        // For Jackson
    }

    @Override
    public String getAvslagskode() {
        return avslagskode;
    }

    @Override
    public boolean getErVilkarOk() {
        return erVilkarOk;
    }

    public LocalDate getMedlemFom() {
        return medlemFom;
    }
}
