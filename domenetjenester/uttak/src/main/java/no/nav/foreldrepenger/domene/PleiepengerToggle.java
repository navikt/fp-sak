package no.nav.foreldrepenger.domene;

import no.nav.foreldrepenger.konfig.Environment;

public final class PleiepengerToggle {

    public static boolean erToggletPå() {
        return !Environment.current().isProd();
    }
}
