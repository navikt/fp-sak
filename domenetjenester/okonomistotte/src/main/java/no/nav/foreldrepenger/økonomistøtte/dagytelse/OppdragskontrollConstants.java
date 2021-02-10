package no.nav.foreldrepenger.økonomistøtte.dagytelse;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndring;

public class OppdragskontrollConstants {

    public static final int POSITIV_KVITTERING = 4;

    public static final String KODE_ENDRING_NY = ØkonomiKodeEndring.NY.name();
    public static final String KODE_ENDRING_UENDRET = ØkonomiKodeEndring.UEND.name();
    public static final String KODE_ENDRING_ENDRET = ØkonomiKodeEndring.ENDR.name();
    public static final String OMPOSTERING_J = "J";
    public static final String OMPOSTERING_N = "N";

    private OppdragskontrollConstants() {
    }
}
