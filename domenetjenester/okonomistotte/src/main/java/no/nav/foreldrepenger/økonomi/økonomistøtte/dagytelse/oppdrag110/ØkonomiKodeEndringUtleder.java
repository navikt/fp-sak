package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdrag110;

import no.nav.foreldrepenger.økonomi.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.FinnMottakerInfoITilkjentYtelse;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.OppdragskontrollConstants;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragInput;

class ØkonomiKodeEndringUtleder {

    private ØkonomiKodeEndringUtleder() {
        // skjul default constructor
    }

    static String finnKodeEndring(OppdragInput behandlingInfo, Oppdragsmottaker mottaker, boolean erNyMottakerIEndring) {
        return mottaker.erBruker()
            ? finnKodeEndringForBruker(behandlingInfo, mottaker, erNyMottakerIEndring)
            : finnKodeEndringForArbeidsgiver(behandlingInfo, erNyMottakerIEndring);
    }

    private static String finnKodeEndringForBruker(OppdragInput behandlingInfo, Oppdragsmottaker mottaker, boolean erNyMottakerIEndring) {
        if (erNyMottakerIEndring) {
            return OppdragskontrollConstants.KODE_ENDRING_NY;
        }
        if (mottaker.erStatusOpphør()) {
            return OppdragskontrollConstants.KODE_ENDRING_UENDRET;
        }
        return mottaker.erStatusEndret() && FinnMottakerInfoITilkjentYtelse.erBrukerMottakerIForrigeTilkjentYtelse(behandlingInfo)
            ? OppdragskontrollConstants.KODE_ENDRING_ENDRET
            : OppdragskontrollConstants.KODE_ENDRING_UENDRET;
    }

    private static String finnKodeEndringForArbeidsgiver(OppdragInput behandlingInfo,
                                                         boolean erNyMottakerIEndring) {
        if (behandlingInfo.gjelderOpphør()) {
            boolean erOpphørEtterStp = behandlingInfo.erOpphørEtterStpEllerIkkeOpphør();
            return erOpphørEtterStp && erNyMottakerIEndring
                ? OppdragskontrollConstants.KODE_ENDRING_NY
                : OppdragskontrollConstants.KODE_ENDRING_UENDRET;
        }
        return erNyMottakerIEndring
            ? OppdragskontrollConstants.KODE_ENDRING_NY
            : OppdragskontrollConstants.KODE_ENDRING_UENDRET;
    }
}
