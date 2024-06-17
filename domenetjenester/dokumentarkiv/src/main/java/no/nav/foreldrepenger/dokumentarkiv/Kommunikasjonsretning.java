package no.nav.foreldrepenger.dokumentarkiv;

import java.util.Map;

public enum Kommunikasjonsretning {
    /**
     * Inngående dokument
     */
    INN("I"),
    /**
     * Utgående dokument
     */
    UT("U"),
    /**
     * Internt notat
     */
    NOTAT("N");

    private static final Map<String, Kommunikasjonsretning> KOMMUNIKASJONSRETNING_MAP = Map.ofEntries(Map.entry(INN.kommunikasjonsretningCode, INN),
        Map.entry(UT.kommunikasjonsretningCode, UT), Map.entry(NOTAT.kommunikasjonsretningCode, NOTAT));

    private String kommunikasjonsretningCode;

    Kommunikasjonsretning(String kommunikasjonsretningCode) {
        this.kommunikasjonsretningCode = kommunikasjonsretningCode;
    }

    public static Kommunikasjonsretning fromKommunikasjonsretningCode(String kommunikasjonsretningCode) {
        return KOMMUNIKASJONSRETNING_MAP.get(kommunikasjonsretningCode);
    }
}
