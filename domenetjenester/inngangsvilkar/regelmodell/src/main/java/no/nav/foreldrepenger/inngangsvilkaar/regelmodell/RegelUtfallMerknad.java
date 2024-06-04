package no.nav.foreldrepenger.inngangsvilkaar.regelmodell;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.SjekkTilstrekkeligOpptjeningInklAntatt;

public enum RegelUtfallMerknad {
    RVM_1001("1001", "Søknad er sendt før 26. svangerskapsuke er passert og barnet er ikke født"),
    RVM_1002("1002", "Søker er medmor (forelder2) og har søkt om engangsstønad til mor"),
    RVM_1003("1003", "Søker er far og har søkt om engangsstønad til mor"),
    RVM_1004("1004", "Barn over 15 år ved dato for omsorgsovertakelse"),
    RVM_1005("1005", "Adopsjon av ektefellens barn"),
    RVM_1006("1006", "Mann adopterer ikke alene"),

    RVM_1019("1019", "Terminbekreftelse utstedt før 22. svangerskapsuke"),

    RVM_1020("1020", "Bruker er registrert som ikke medlem"),
    RVM_1023("1023", "Bruker ikke er registrert som norsk eller nordisk statsborger i TPS OG bruker ikke er registrert som borger av EU/EØS OG det ikke er avklart at bruker har lovlig opphold i Norge"),
    RVM_1024("1024", "Bruker ikke er registrert som norsk eller nordisk statsborger i TPS OG bruker er registrert som borger av EU/EØS OG det ikke er avklart at bruker har oppholdsrett"),
    RVM_1025("1025", "Bruker avklart som ikke bosatt."),

    RVM_1026("1026", "Fødselsdato ikke oppgitt eller registrert"),
    RVM_1027("1027", "ingen barn dokumentert på far/medmor"),
    RVM_1028("1028", "mor fyller ikke vilkåret for sykdom"),

    RVM_1035(SjekkTilstrekkeligOpptjeningInklAntatt.IKKE_TILSTREKKELIG_OPPTJENING_ID, "Ikke tilstrekkelig opptjening"),

    UDEFINERT("-", "Ikke definert"),

    ;

    private String navn;

    private String kode;

    RegelUtfallMerknad(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public String getNavn() {
        return navn;
    }

    public String getKode() {
        return kode;
    }

}
