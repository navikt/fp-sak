package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

public enum FagsakÅrsak {
    FØDSEL("Fødsel"),
    ADOPSJON("Adopsjon"),
    OMSORG("Omsorgsovertakelse"),
    SVANGERSKAP("Svangerskap");

    private String kode;

    FagsakÅrsak(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
