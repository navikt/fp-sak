package no.nav.foreldrepenger.web.app.util;

import java.util.Locale;

public class StringUtils {

    private StringUtils() {}

    public static boolean erIkkeTom(String str) {
        return (str != null) && (str.length() > 0);
    }

    public static boolean erTom(String str) {
        return !StringUtils.erIkkeTom(str);
    }

    public static String formaterMedStoreOgSmåBokstaver(String tekst) {
        if (tekst == null || (tekst = tekst.trim()).isEmpty()) { // NOSONAR
            return null;
        }
        String skilletegnPattern = "(\\s|[()\\-_.,/])";
        char[] tegn = tekst.toLowerCase(Locale.getDefault()).toCharArray();
        boolean nesteSkalHaStorBokstav = true;
        for (int i = 0; i < tegn.length; i++) {
            boolean erSkilletegn = String.valueOf(tegn[i]).matches(skilletegnPattern);
            if (!erSkilletegn && nesteSkalHaStorBokstav) {
                tegn[i] = Character.toTitleCase(tegn[i]);
            }
            nesteSkalHaStorBokstav = erSkilletegn;
        }
        return new String(tegn);
    }

}
