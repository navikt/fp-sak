package no.nav.foreldrepenger.behandling.klage;

import java.util.Optional;

public class KlageVurderingAdapter {
    private String klageVurderingKode;
    private String begrunnelse;
    private String fritekstTilBrev;
    private String klageMedholdArsakKode;
    private String klageVurderingOmgjoer;
    private boolean erNfpAksjonspunkt;
    private boolean erGodkjentAvMedunderskriver;

    public KlageVurderingAdapter(String klageVurderingKode, String begrunnelse,
                                 String klageMedholdArsakKode, boolean erNfpAksjonspunkt,
                                 String fritekstTilBrev, String klageVurderingOmgjør,
                                 boolean erGodkjentAvMedunderskriver) {
        this.klageVurderingKode = klageVurderingKode;
        this.begrunnelse = begrunnelse;
        this.fritekstTilBrev = fritekstTilBrev;
        this.klageMedholdArsakKode = klageMedholdArsakKode;
        this.klageVurderingOmgjoer = klageVurderingOmgjør;
        this.erNfpAksjonspunkt = erNfpAksjonspunkt;
        this.erGodkjentAvMedunderskriver = erGodkjentAvMedunderskriver;
    }

    public String getKlageVurderingKode() {
        return klageVurderingKode;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstTilBrev() {
        return fritekstTilBrev;
    }

    public Optional<String> getKlageMedholdArsakKode() {
        return Optional.ofNullable(klageMedholdArsakKode);
    }

    public Optional<String> getKlageVurderingOmgjoer() {
        return Optional.ofNullable(klageVurderingOmgjoer);
    }

    public boolean getErNfpAksjonspunkt() {
        return erNfpAksjonspunkt;
    }

    public boolean getErGodkjentAvMedunderskriver() {
        return erGodkjentAvMedunderskriver;
    }
}
