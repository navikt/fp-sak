package no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdrag110;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.FinnMottakerInfoITilkjentYtelse;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragInput;

class ØkonomiKodeEndringUtleder {

    private ØkonomiKodeEndringUtleder() {
        // skjul default constructor
    }

    static KodeEndring finnKodeEndring(OppdragInput behandlingInfo, Oppdragsmottaker mottaker, boolean erNyMottakerIEndring) {
        return mottaker.erBruker()
            ? finnKodeEndringForBruker(behandlingInfo, mottaker, erNyMottakerIEndring)
            : finnKodeEndringForArbeidsgiver(erNyMottakerIEndring);
    }

    private static KodeEndring finnKodeEndringForBruker(OppdragInput behandlingInfo, Oppdragsmottaker mottaker, boolean erNyMottakerIEndring) {
        if (erNyMottakerIEndring) {
            return KodeEndring.NY;
        }
        if (mottaker.erStatusOpphør()) {
            return KodeEndring.UENDRET;
        }
        return mottaker.erStatusEndret() && FinnMottakerInfoITilkjentYtelse.erBrukerMottakerIForrigeTilkjentYtelse(behandlingInfo)
            ? KodeEndring.ENDRING
            : KodeEndring.UENDRET;
    }

    private static KodeEndring finnKodeEndringForArbeidsgiver(boolean erNyMottakerIEndring) {
        return erNyMottakerIEndring
            ? KodeEndring.NY
            : KodeEndring.UENDRET;
    }
}
