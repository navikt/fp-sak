package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;

@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class KlageVurderingResultatDto {

    @JsonProperty("klageVurdering")
    private KlageVurdering klageVurdering;
    @JsonProperty("begrunnelse")
    private String begrunnelse;
    @JsonProperty("fritekstTilBrev")
    private String fritekstTilBrev;
    @JsonProperty("klageMedholdArsak")
    private KlageMedholdÅrsak klageMedholdArsak;
    @JsonProperty("klageVurderingOmgjoer")
    private KlageVurderingOmgjør klageVurderingOmgjoer;
    @JsonProperty("klageVurdertAv")
    private String klageVurdertAv;
    @JsonProperty("godkjentAvMedunderskriver")
    private boolean godkjentAvMedunderskriver;

    public KlageVurderingResultatDto() {
    }

    public KlageVurdering getKlageVurdering() {
        return klageVurdering;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstTilBrev() {
        return fritekstTilBrev;
    }

    public KlageMedholdÅrsak getKlageMedholdArsak() {
        return klageMedholdArsak;
    }

    public String getKlageVurdertAv() {
        return klageVurdertAv;
    }

    public KlageVurderingOmgjør getKlageVurderingOmgjoer() {
        return klageVurderingOmgjoer;
    }

    public boolean isGodkjentAvMedunderskriver() {
        return godkjentAvMedunderskriver;
    }

    public void setKlageVurderingOmgjoer(KlageVurderingOmgjør klageVurderingOmgjoer) {
        this.klageVurderingOmgjoer = klageVurderingOmgjoer;
    }

    void setKlageVurdering(KlageVurdering klageVurdering) {
        this.klageVurdering = klageVurdering;
    }

    void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setFritekstTilBrev(String fritekstTilBrev) {
        this.fritekstTilBrev = fritekstTilBrev;
    }

    void setKlageMedholdArsak(KlageMedholdÅrsak klageMedholdArsak) {
        this.klageMedholdArsak = klageMedholdArsak;
    }

    void setKlageVurdertAv(String klageVurdertAv) {
        this.klageVurdertAv = klageVurdertAv;
    }

    public void setGodkjentAvMedunderskriver(boolean godkjentAvMedunderskriver) {
        this.godkjentAvMedunderskriver = godkjentAvMedunderskriver;
    }
}
