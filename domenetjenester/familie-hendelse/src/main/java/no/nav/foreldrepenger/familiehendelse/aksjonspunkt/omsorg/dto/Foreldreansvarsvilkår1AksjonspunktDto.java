package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorg.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AvslagbartAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;

@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_2_LEDD_KODE)
public class Foreldreansvarsvilkår1AksjonspunktDto extends BekreftetAksjonspunktDto implements AvslagbartAksjonspunktDto {


    @NotNull
    private Boolean erVilkarOk;

    @Size(min = 1, max = 100)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String avslagskode;

    Foreldreansvarsvilkår1AksjonspunktDto() {
        //For Jackson
    }

    public Foreldreansvarsvilkår1AksjonspunktDto(String begrunnelse, Boolean erVilkarOk, String avslagskode) {
        super(begrunnelse);
        this.erVilkarOk = erVilkarOk;
        this.avslagskode = avslagskode;
    }

    @Override
    public Boolean getErVilkarOk() {
        return erVilkarOk;
    }

    @Override
    public String getAvslagskode() {
        return avslagskode;
    }


}
