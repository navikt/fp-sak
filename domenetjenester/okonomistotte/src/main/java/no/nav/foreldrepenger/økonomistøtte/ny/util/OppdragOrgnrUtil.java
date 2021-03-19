package no.nav.foreldrepenger.økonomistøtte.ny.util;

public class OppdragOrgnrUtil {

    private static final String REFUNDERES_ID_PREFIX = "00";
    private static final int REFUNDERES_ID_LENGDE = 11;

    public static String endreTilElleveSiffer(String id) {
        if (id.length() == REFUNDERES_ID_LENGDE) {
            return id;
        }
        return REFUNDERES_ID_PREFIX + id;
    }

    public static String endreTilNiSiffer(String refunderesId) {
        if (refunderesId.length() == REFUNDERES_ID_LENGDE) {
            return refunderesId.substring(2);
        }
        return refunderesId;
    }
}
