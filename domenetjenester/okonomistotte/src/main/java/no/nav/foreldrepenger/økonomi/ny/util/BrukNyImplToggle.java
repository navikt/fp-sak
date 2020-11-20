package no.nav.foreldrepenger.økonomi.ny.util;

import no.nav.vedtak.util.env.Environment;

public class BrukNyImplToggle {
    public static boolean brukNyImpl() {
        return !Environment.current().isProd();
    }
}
