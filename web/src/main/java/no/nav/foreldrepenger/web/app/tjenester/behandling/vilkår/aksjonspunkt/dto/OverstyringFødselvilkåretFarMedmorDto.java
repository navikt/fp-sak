package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;

@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_FØDSELSVILKÅRET_FAR_MEDMOR_KODE)
public class OverstyringFødselvilkåretFarMedmorDto extends OverstyringAksjonspunktDto {

    @Size(min = 4, max = 4)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String avslagskode;

    private boolean erVilkårOk;

    @SuppressWarnings("unused")
    private OverstyringFødselvilkåretFarMedmorDto() {
        super();
        // For Jackson
    }

    public OverstyringFødselvilkåretFarMedmorDto(boolean erVilkårOk, String begrunnelse, String avslagskode) {
        super(begrunnelse);
        this.erVilkårOk = erVilkårOk;
        this.avslagskode = avslagskode;
    }
    @Override
    public String getAvslagskode() {
        return avslagskode;
    }

    @Override
    public boolean getErVilkårOk() {
        return erVilkårOk;
    }
}
