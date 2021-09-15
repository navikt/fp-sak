package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap;

public enum RegelPersonStatusType {
    BOSA("Bosatt"),
    UTVA("Utvandret"),
    DØD("Død");

    private String navn; // TODO erstatt med name + bruke fulltekst enum når VedtakXML landet (lese gammel json)

    RegelPersonStatusType(String navn) {
        this.navn = navn;
    }

    public String getNavn() {
        return navn;
    }
}
