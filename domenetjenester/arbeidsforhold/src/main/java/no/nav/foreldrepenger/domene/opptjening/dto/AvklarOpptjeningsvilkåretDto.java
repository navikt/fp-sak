package no.nav.foreldrepenger.domene.opptjening.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_OPPTJENINGSVILKÅRET_KODE)
public class AvklarOpptjeningsvilkåretDto extends BekreftetAksjonspunktDto {

    @JsonProperty("avslagskode")
    @Size(min = 4, max = 4)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String avslagskode;

    @JsonProperty("erVilkarOk")
    private boolean erVilkarOk;

    @SuppressWarnings("unused")
    private AvklarOpptjeningsvilkåretDto() {
        super();
        // For Jackson
    }

    public AvklarOpptjeningsvilkåretDto(String begrunnelse,
            @Size(min = 4, max = 4) @Pattern(regexp = InputValideringRegex.KODEVERK) String avslagskode, boolean erVilkarOk) {
        super(begrunnelse);
        this.avslagskode = avslagskode;
        this.erVilkarOk = erVilkarOk;
    }

    public String getAvslagskode() {
        return avslagskode;
    }

    public boolean getErVilkarOk() {
        return erVilkarOk;
    }
}
