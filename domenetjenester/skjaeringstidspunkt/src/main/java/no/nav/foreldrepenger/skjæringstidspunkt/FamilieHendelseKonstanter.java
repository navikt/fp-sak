package no.nav.foreldrepenger.skjæringstidspunkt;

import java.time.LocalDate;
import java.time.Period;

public class FamilieHendelseKonstanter {

    public static final Period TERMINBEKREFTELSE_TIDLIGST_UTSTEDT = Period.parse("P18W3D");
    public static final int ADOPSJON_ALDERSGRENSE = 15; // Omsorgsovertakelsedato før barnet er 15 år.

    /**
     * Fra og med man er i svangerskapsuke 22 kan man søke og få innvilget ES/FP.
     * Dette er tolket som FOMdato = termindato - 18uker - 3dager
     */
    public static boolean erTerminbekreftelseUtstedtForTidlig(LocalDate termindato, LocalDate utstedtDato) {
        if (termindato == null || utstedtDato == null) {
            return false;
        }
        var tidligstedato = termindato.minus(TERMINBEKREFTELSE_TIDLIGST_UTSTEDT);
        return utstedtDato.isBefore(tidligstedato);
    }

    public static boolean erTerminbekreftelseUtstedtSentNok(LocalDate termindato, LocalDate utstedtDato) {
        if (termindato == null || utstedtDato == null) {
            return true;
        }
        var tidligstedato = termindato.minus(TERMINBEKREFTELSE_TIDLIGST_UTSTEDT);
        return utstedtDato.isAfter(tidligstedato) || utstedtDato.equals(tidligstedato);
    }

    private FamilieHendelseKonstanter() {
    }

}
