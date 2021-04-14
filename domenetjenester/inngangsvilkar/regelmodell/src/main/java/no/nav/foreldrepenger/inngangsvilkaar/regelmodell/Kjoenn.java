package no.nav.foreldrepenger.inngangsvilkaar.regelmodell;

public enum Kjoenn {
    KVINNE("K"), MANN("M");

    private String kode;

    Kjoenn(String kode) {
        this.kode = kode;
    }

    public static Kjoenn hentKjoenn(String kode) {
        for (var kjoenn : values()) {
            if (kjoenn.kode.equals(kode)) {
                return kjoenn;
            }
        }
        return null;
    }

    public String getKode() {
        return kode;
    }
}

