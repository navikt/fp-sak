package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.sammebarn.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE_KODE)
public class VurdereYtelseSammeBarnSøkerAksjonspunktDto extends BekreftetAksjonspunktDto {

    @NotNull
    private boolean erVilkårOk;

    @Size(min = 1, max = 100)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String avslagskode;

    public VurdereYtelseSammeBarnSøkerAksjonspunktDto() {
        // for jackson
    }

    public VurdereYtelseSammeBarnSøkerAksjonspunktDto(String begrunnelse, Boolean erVilkårOk) {
        super(begrunnelse);
        this.erVilkårOk = erVilkårOk;
    }

    public boolean getErVilkårOk() {
        return erVilkårOk;
    }

    public void setErVilkårOk(Boolean erVilkårOk) {
        this.erVilkårOk = erVilkårOk;
    }

    public String getAvslagskode() {
        return avslagskode;
    }

    public void setAvslagskode(String avslagskode) {
        this.avslagskode = avslagskode;
    }

}
