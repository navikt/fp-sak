package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.Rettighetstype;

@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_RETT_OG_OMSORG_KODE)
public class OverstyrOmsorgOgRettDto extends OverstyringAksjonspunktDto {

    @NotNull
    @Valid
    private Rettighetstype rettighetstype;

    OverstyrOmsorgOgRettDto() {
        // For Jackson
    }

    public OverstyrOmsorgOgRettDto(String begrunnelse, Rettighetstype rettighetstype) {
        super(begrunnelse);
        this.rettighetstype = rettighetstype;
    }

    public Rettighetstype getRettighetstype() {
        return rettighetstype;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        //Brukes ikke
        return null;
    }

    @JsonIgnore
    @Override
    public boolean getErVilkarOk() {
        //Brukes ikke
        return false;
    }
}
