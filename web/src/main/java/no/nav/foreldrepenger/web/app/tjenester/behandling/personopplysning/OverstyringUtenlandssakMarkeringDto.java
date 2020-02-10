package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE_KODE)
public class OverstyringUtenlandssakMarkeringDto extends OverstyringAksjonspunktDto {


    @JsonProperty("gammelVerdi")
    @Size(max = 20)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String gammelVerdi;

    OverstyringUtenlandssakMarkeringDto() {
        // For Jackson
    }

    public OverstyringUtenlandssakMarkeringDto(String begrunnelse, String gammelVerdi) {
        super(begrunnelse);
        this.gammelVerdi = gammelVerdi;
    }

    @JsonGetter
    public String getGammelVerdi() {
        return gammelVerdi;
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
