package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.kafka;

public class AnnenPart {

    private AktoerIdDto annenPartAktoerId;

    private String utenlandskFnr;

    public AnnenPart(AktoerIdDto annenPartAktoerId){
        this.annenPartAktoerId = annenPartAktoerId;
        this.utenlandskFnr = null;
    }

    public AnnenPart(String utenlandskFnr){
        this.annenPartAktoerId = null;
        this.utenlandskFnr = utenlandskFnr;
    }

    public AktoerIdDto getAnnenPartAktoerId() {
        return annenPartAktoerId;
    }

    public String getUtenlandskFnr() {
        return utenlandskFnr;
    }
}
