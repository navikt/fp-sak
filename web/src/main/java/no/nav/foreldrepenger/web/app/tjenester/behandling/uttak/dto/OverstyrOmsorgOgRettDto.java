package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.RettighetType;

@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_RETT_OG_OMSORG)
public class OverstyrOmsorgOgRettDto extends OverstyringAksjonspunktDto {

    @NotNull
    private RettighetType rettighet;

    OverstyrOmsorgOgRettDto() {
        // For Jackson
    }

    public OverstyrOmsorgOgRettDto(String begrunnelse, RettighetType rettighet) {
        super(begrunnelse);
        this.rettighet = rettighet;
    }

    public RettighetType getRettighet() {
        return rettighet;
    }

    public void setRettighet(RettighetType rettighet) {
        this.rettighet = rettighet;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        return null;
    }

    @JsonIgnore
    @Override
    public boolean getErVilkarOk() {
        return false; // TODO: Hva skal denne være?
    }
}
