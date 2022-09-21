package no.nav.foreldrepenger.domene.prosess;

import no.nav.foreldrepenger.konfig.Environment;

public class SplittForeslåBgToggle {

    public static boolean erTogglePå() {
        return !Environment.current().isProd();
    }
}
