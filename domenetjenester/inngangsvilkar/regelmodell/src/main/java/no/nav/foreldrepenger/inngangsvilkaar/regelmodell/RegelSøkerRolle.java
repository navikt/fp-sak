package no.nav.foreldrepenger.inngangsvilkaar.regelmodell;

public enum RegelSøkerRolle {
    MORA("Mor til"),
    MEDMOR("Medmor til"),
    FARA("Far til");

    private String kode;

    RegelSøkerRolle(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
