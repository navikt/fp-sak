package no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;

@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_ANKE_MERKNADER_KODE)
@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class AnkeMerknaderResultatAksjonspunktDto extends BekreftetAksjonspunktDto {

    @JsonProperty("erMerknaderMottatt")
    private boolean erMerknaderMottatt;

    @JsonProperty("avsluttBehandling")
    private boolean avsluttBehandling;

    @Size(max = 100000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    @JsonProperty("merknadKommentar")
    private String merknadKommentar;

    AnkeMerknaderResultatAksjonspunktDto() {
        // For Jackson
    }

    public AnkeMerknaderResultatAksjonspunktDto (boolean avsluttBehandling,
                                                 boolean erMerknaderMottatt,
                                                 String merknadKommentar){
        this.erMerknaderMottatt = erMerknaderMottatt;
        this.merknadKommentar = merknadKommentar;
        this.avsluttBehandling =avsluttBehandling;
    }

    public boolean erMerknaderMottatt() {
        return erMerknaderMottatt;
    }

    public String getMerknadKommentar() {
        return merknadKommentar;
    }

    public boolean skalAvslutteBehandling() {
        return avsluttBehandling;
    }
}
