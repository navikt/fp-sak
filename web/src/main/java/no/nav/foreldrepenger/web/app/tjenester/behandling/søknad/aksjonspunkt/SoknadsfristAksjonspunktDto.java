package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AvslagbartAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.vedtak.util.InputValideringRegex;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET_KODE)
public class SoknadsfristAksjonspunktDto extends BekreftetAksjonspunktDto implements AvslagbartAksjonspunktDto {

    @NotNull
    private boolean erVilkårOk;

    @Size(min = 4, max = 4)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String avslagskode;

    SoknadsfristAksjonspunktDto() {
        //For Jackson
    }

    public SoknadsfristAksjonspunktDto(boolean erVilkårOk, String begrunnelse, String avslagskode) {
        super(begrunnelse);
        this.erVilkårOk = erVilkårOk;
        this.avslagskode = avslagskode;
    }

    @Override
    public boolean getErVilkårOk() {
        return erVilkårOk;
    }

    // TODO: fjerne denne håndteringen når frontend er oppdatert til å sende med avslagskode
    @Override
    public String getAvslagskode() {
        return avslagskode == null && !erVilkårOk ? Avslagsårsak.SØKT_FOR_SENT.getKode() : avslagskode;
    }

}
