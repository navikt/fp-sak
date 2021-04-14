package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap;

public enum PersonStatusType {
    BOSA("Bosatt"),
    UTVA("Utvandret"),
    DØD("Død");

    private String kode;

    PersonStatusType(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }

    public static PersonStatusType hentPersonStatusType(String kode) {
        for (var personStatusType : values()) {
            if (personStatusType.kode.equals(kode)) {
                return personStatusType;
            }
        }
        return null;
    }
}
