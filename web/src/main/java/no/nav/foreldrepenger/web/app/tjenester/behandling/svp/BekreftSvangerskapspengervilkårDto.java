package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;

@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET_KODE)
public class BekreftSvangerskapspengervilkårDto extends BekreftetAksjonspunktDto {


    @JsonProperty("avslagskode")
    @Size(min = 4, max = 4)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String avslagskode;

    BekreftSvangerskapspengervilkårDto() {

    }

    public BekreftSvangerskapspengervilkårDto(String begrunnelse, String avslagskode) {
        super(begrunnelse);
        this.avslagskode = avslagskode;
    }


    public String getAvslagskode() {
        return avslagskode;
    }
}
