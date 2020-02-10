package no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;

@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_ANKE_MERKNADER_KODE)
public class AnkeMerknaderResultatAksjonspunktDto extends BekreftetAksjonspunktDto {


    @NotNull
    @JsonProperty("erMerknaderMottatt")
    private boolean erMerknaderMottatt;

    @Size(max = 100000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String merknadKommentar;

    AnkeMerknaderResultatAksjonspunktDto() { // NOSONAR
        // For Jackson
    }

    public AnkeMerknaderResultatAksjonspunktDto (// NOSONAR
                                                 boolean erMerknaderMottatt,
                                                 String merknadKommentar){
        this.erMerknaderMottatt = erMerknaderMottatt;
        this.merknadKommentar = merknadKommentar;
    }

    public boolean erMerknaderMottatt() {
        return erMerknaderMottatt;
    }

    public String getMerknadKommentar() {
        return merknadKommentar;
    }

}
