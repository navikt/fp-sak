package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

public class KlagebehandlingDto {

    private KlageVurderingResultatDto klageVurderingResultatNFP;
    private KlageVurderingResultatDto klageVurderingResultatNK;
    private KlageFormkravResultatDto klageFormkravResultatNFP;
    private KlageFormkravResultatDto klageFormkravResultatKA;


    public KlagebehandlingDto() {
        // trengs for deserialisering av JSON
    }
    public KlageVurderingResultatDto getKlageVurderingResultatNFP() {
        return klageVurderingResultatNFP;
    }

    public KlageVurderingResultatDto getKlageVurderingResultatNK() {
        return klageVurderingResultatNK;
    }

    public KlageFormkravResultatDto getKlageFormkravResultatNFP() { return klageFormkravResultatNFP; }

    public KlageFormkravResultatDto getKlageFormkravResultatKA() { return klageFormkravResultatKA;}

    void setKlageVurderingResultatNFP(KlageVurderingResultatDto klageVurderingResultatNFP) {
        this.klageVurderingResultatNFP = klageVurderingResultatNFP;
    }

    void setKlageVurderingResultatNK(KlageVurderingResultatDto klageVurderingResultatNK) {
        this.klageVurderingResultatNK = klageVurderingResultatNK;
    }

    void setKlageFormkravResultatNFP(KlageFormkravResultatDto klageFormkravResultatNFP){
        this.klageFormkravResultatNFP = klageFormkravResultatNFP;
    }

    void setKlageFormkravResultatKA(KlageFormkravResultatDto klageFormkravResultatKA){
        this.klageFormkravResultatKA = klageFormkravResultatKA;
    }
}
