package no.nav.foreldrepenger.økonomistøtte.dagytelse;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiTypeSats;

public class OppdragskontrollConstants {

    public static final int POSITIV_KVITTERING = 4;

    public static final String KODE_ENDRING_NY = ØkonomiKodeEndring.NY.name();
    public static final String KODE_ENDRING_UENDRET = ØkonomiKodeEndring.UEND.name();
    public static final String KODE_ENDRING_ENDRET = ØkonomiKodeEndring.ENDR.name();
    public static final String TYPE_SATS_DAG = ØkonomiTypeSats.DAG.name();
    public static final String TYPE_SATS_FERIEPENGER = ØkonomiTypeSats.ENG.name();
    public static final String OMPOSTERING_J = "J";
    public static final String OMPOSTERING_N = "N";
    public static final String KODE_STATUS_LINJE_OPPHØR = ØkonomiKodeStatusLinje.OPPH.name();

    private OppdragskontrollConstants() {
    }
}
