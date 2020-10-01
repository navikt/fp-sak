package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class KlagebehandlingDto {

    @JsonProperty("klageVurderingResultatNFP")
    private KlageVurderingResultatDto klageVurderingResultatNFP;
    @JsonProperty("klageVurderingResultatNK")
    private KlageVurderingResultatDto klageVurderingResultatNK;
    @JsonProperty("klageFormkravResultatNFP")
    private KlageFormkravResultatDto klageFormkravResultatNFP;
    @JsonProperty("klageFormkravResultatKA")
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
