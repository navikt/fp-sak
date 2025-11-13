package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.vedtak.util.InputValideringRegex;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_SØKNADSFRISTVILKÅRET_KODE)
public class OverstyringSøknadsfristvilkåretDto extends OverstyringAksjonspunktDto {

    @NotNull
    private boolean erVilkarOk;

    @Size(min = 4, max = 4)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String avslagskode;

    @SuppressWarnings("unused")
    private OverstyringSøknadsfristvilkåretDto() {
        super();
        // For Jackson
    }

    public OverstyringSøknadsfristvilkåretDto(boolean erVilkarOk, String begrunnelse, String avslagskode) {
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
