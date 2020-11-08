package no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.validering.ValidKodeverk;
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

    @ValidKodeverk
    @JsonProperty("trygderettVurdering")
    private AnkeVurdering trygderettVurdering;

    @ValidKodeverk
    @JsonProperty("trygderettOmgjoerArsak")
    private AnkeOmgjørÅrsak trygderettOmgjoerArsak;

    @ValidKodeverk
    @JsonProperty("trygderettVurderingOmgjoer")
    private AnkeVurderingOmgjør trygderettVurderingOmgjoer;


    AnkeMerknaderResultatAksjonspunktDto() {
        // For Jackson
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

    public AnkeVurdering getTrygderettVurdering() {
        return trygderettVurdering;
    }

    public AnkeOmgjørÅrsak getTrygderettOmgjoerArsak() {
        return trygderettOmgjoerArsak;
    }

    public AnkeVurderingOmgjør getTrygderettVurderingOmgjoer() {
        return trygderettVurderingOmgjoer;
    }
}
