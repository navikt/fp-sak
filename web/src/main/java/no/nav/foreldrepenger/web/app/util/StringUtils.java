package no.nav.foreldrepenger.web.app.util;

import java.util.Locale;

public class StringUtils {

    private StringUtils() {}

    public static boolean erIkkeTom(String str) {
        return str != null && str.length() > 0;
    }

    public static boolean erTom(String str) {
        return !StringUtils.erIkkeTom(str);
    }

    public static String formaterMedStoreOgSmåBokstaver(String tekst) {
        if (tekst == null || (tekst = tekst.trim()).isEmpty()) {
            return null;
        }
        var skilletegnPattern = "(\\s|[()\\-_.,/])";
        var tegn = tekst.toLowerCase(Locale.getDefault()).toCharArray();
        var nesteSkalHaStorBokstav = true;
        for (var i = 0; i < tegn.length; i++) {
            var erSkilletegn = String.valueOf(tegn[i]).matches(skilletegnPattern);
            if (!erSkilletegn && nesteSkalHaStorBokstav) {
                tegn[i] = Character.toTitleCase(tegn[i]);
            }
            nesteSkalHaStorBokstav = erSkilletegn;
        }
        return new String(tegn);
    }

}
