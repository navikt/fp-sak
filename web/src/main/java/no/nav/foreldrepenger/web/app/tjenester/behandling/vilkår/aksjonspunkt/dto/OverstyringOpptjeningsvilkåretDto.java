package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;


@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_OPPTJENINGSVILKÅRET_KODE)
public class OverstyringOpptjeningsvilkåretDto extends OverstyringAksjonspunktDto {


    @JsonProperty("avslagskode")
    @Size(min = 4, max = 4)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String avslagskode;

    @JsonProperty("erVilkarOk")
    private boolean erVilkarOk;

    @SuppressWarnings("unused") // NOSONAR
    private OverstyringOpptjeningsvilkåretDto() {
        super();
        // For Jackson
    }

    public OverstyringOpptjeningsvilkåretDto(boolean erVilkarOk, String begrunnelse, String avslagskode) { // NOSONAR
        super(begrunnelse);
        this.erVilkarOk = erVilkarOk;
        this.avslagskode = avslagskode;
    }

    @Override
    public String getAvslagskode() {
        return avslagskode;
    }

    @Override
    public boolean getErVilkarOk() {
        return erVilkarOk;
    }

}
