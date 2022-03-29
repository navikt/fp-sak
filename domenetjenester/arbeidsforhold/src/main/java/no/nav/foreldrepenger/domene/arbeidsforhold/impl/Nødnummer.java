package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.util.ArrayList;

/**
 * Alle kjente nødnummer i infotrygd.
 */
public enum Nødnummer {

    NØDNUMMER_FOR_TRYGDEETATEN("973626108"),
    NØDNUMMER_FOR_TRYGDEETATEN_2("973626116"),
    NØDNUMMER_FOR_TRYGDEETATEN_3("971278420"),
    NØDNUMMER_FOR_TRYGDEETATEN_4("971278439"),
    NØDNUMMER_FOR_TRYGDEETATEN_5("971248106"),
    NØDNUMMER_FOR_TRYGDEETATEN_6("971373032"),
    NØDNUMMER_FOR_TRYGDEETATEN_FISKER_MED_HYRE("871400172"),
    NØDNUMMER_FREDRIKSTAD_TRYGDEKONTOR("973695061"),
    NØDNUMMER_TROMSØ_TRYGDEKONTOR("973540017");

    private final String nødnummer;

    private static final ArrayList<String> LISTE_MED_NØDNUMMER = new ArrayList<>();

    static {
        for (var a : Nødnummer.values()) {
            LISTE_MED_NØDNUMMER.add(a.getNødnummer());
        }
    }

    Nødnummer(String nødnummer) {
        this.nødnummer = nødnummer;
    }

    public static boolean erNødnummer(String identifikator) {
        return identifikator != null && LISTE_MED_NØDNUMMER.contains(identifikator);
    }

    public String getNødnummer() {
        return nødnummer;
    }
}
