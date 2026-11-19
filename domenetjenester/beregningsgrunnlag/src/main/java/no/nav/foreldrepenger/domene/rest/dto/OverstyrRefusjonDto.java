package no.nav.foreldrepenger.domene.rest.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.håndtering.refusjon.RefusjonOverstyringAndel;

@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_REFUSJON_KODE)
public class OverstyrRefusjonDto extends OverstyringAksjonspunktDto {

    @NotNull
    private List<@Valid RefusjonOverstyringAndel> overstyrteAndeler;

    @SuppressWarnings("unused")
    private OverstyrRefusjonDto() {
        super();
        // For Jackson
    }

    public OverstyrRefusjonDto(List<RefusjonOverstyringAndel> overstyrteAndeler, String begrunnelse) {
        super(begrunnelse);
        this.overstyrteAndeler = overstyrteAndeler;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        return null;
    }

    @JsonIgnore
    @Override
    public boolean getErVilkårOk() {
        return true;
    }

    public List<RefusjonOverstyringAndel> getOverstyrteAndeler() {
        return overstyrteAndeler;
    }
}
