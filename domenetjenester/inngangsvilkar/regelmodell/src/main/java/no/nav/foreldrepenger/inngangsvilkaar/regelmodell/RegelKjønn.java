package no.nav.foreldrepenger.inngangsvilkaar.regelmodell;

public enum RegelKjønn {
    KVINNE("K"), MANN("M");

    private String kode;

    RegelKjønn(String kode) {
        this.kode = kode;
    }

    public static RegelKjønn hentKjønn(String kode) {
        for (var kjønn : values()) {
            if (kjønn.kode.equals(kode)) {
                return kjønn;
            }
        }
        return null;
    }

    public String getKode() {
        return kode;
    }
}

