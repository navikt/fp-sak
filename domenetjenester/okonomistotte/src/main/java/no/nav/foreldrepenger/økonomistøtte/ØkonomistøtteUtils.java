package no.nav.foreldrepenger.økonomistøtte;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ØkonomistøtteUtils {

    private ØkonomistøtteUtils() {
        // skjul public constructor
    }

    /**
     * Formats the given LocalDateTime into a String with the given pattern yyyy-MM-dd-HH.mm.ss.SSS
     *
     * @param dt - the object to transform
     * @return a formated string.
     */
    public static String tilSpesialkodetDatoOgKlokkeslett(LocalDateTime dt) {
        if (dt == null) {
            return null;
        }
        var pattern = "yyyy-MM-dd-HH.mm.ss.SSS";
        var dtf = DateTimeFormatter.ofPattern(pattern);
        return dt.format(dtf);
    }
}
