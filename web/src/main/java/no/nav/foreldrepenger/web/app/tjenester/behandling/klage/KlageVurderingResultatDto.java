package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

public class KlageVurderingResultatDto {
    private String klageVurdering;
    private String begrunnelse;
    private String fritekstTilBrev;
    private String klageMedholdArsak;
    private String klageMedholdArsakNavn;
    private String klageVurderingOmgjoer;
    private String klageVurdertAv;
    private boolean godkjentAvMedunderskriver;

    public KlageVurderingResultatDto() {
    }

    public String getKlageVurdering() {
        return klageVurdering;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstTilBrev() {
        return fritekstTilBrev;
    }

    public String getKlageMedholdArsak() {
        return klageMedholdArsak;
    }

    public String getKlageVurdertAv() {
        return klageVurdertAv;
    }

    public String getKlageMedholdArsakNavn() {
        return klageMedholdArsakNavn;
    }

    public String getKlageVurderingOmgjoer() {
        return klageVurderingOmgjoer;
    }

    public boolean isGodkjentAvMedunderskriver() {
        return godkjentAvMedunderskriver;
    }

    public void setKlageVurderingOmgjoer(String klageVurderingOmgjoer) {
        this.klageVurderingOmgjoer = klageVurderingOmgjoer;
    }

    void setKlageVurdering(String klageVurdering) {
        this.klageVurdering = klageVurdering;
    }

    void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setFritekstTilBrev(String fritekstTilBrev) {
        this.fritekstTilBrev = fritekstTilBrev;
    }

    void setKlageMedholdArsak(String klageMedholdArsak) {
        this.klageMedholdArsak = klageMedholdArsak;
    }

    void setKlageMedholdArsakNavn(String klageMedholdArsakNavn) {
        this.klageMedholdArsakNavn = klageMedholdArsakNavn;
    }

    void setKlageVurdertAv(String klageVurdertAv) {
        this.klageVurdertAv = klageVurdertAv;
    }

    public void setGodkjentAvMedunderskriver(boolean godkjentAvMedunderskriver) {
        this.godkjentAvMedunderskriver = godkjentAvMedunderskriver;
    }
}
