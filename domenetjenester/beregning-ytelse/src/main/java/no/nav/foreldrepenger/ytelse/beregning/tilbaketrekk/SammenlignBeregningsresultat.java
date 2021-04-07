package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class SammenlignBeregningsresultat {

    public static boolean erLike(BeregningsresultatEntitet res1, BeregningsresultatEntitet res2) {
        if (!perioderErlike(res1.getBeregningsresultatPerioder(), res2.getBeregningsresultatPerioder())) {
            return false;
        }
        if (!res1.getEndringsdato().equals(res2.getEndringsdato())) {
            return false;
        }
        return true;
    }

    private static boolean perioderErlike(List<BeregningsresultatPeriode> liste1, List<BeregningsresultatPeriode> liste2) {
        if (liste1.size() != liste2.size()) {
            return false;
        }
        for (var periode1 : liste1) {
            Optional<BeregningsresultatPeriode> matchendePeriode = liste2.stream()
                .filter(periode2 -> periodeErLik(periode1, periode2))
                .findFirst();
            if (matchendePeriode.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean periodeErLik(BeregningsresultatPeriode p1, BeregningsresultatPeriode p2) {
        if (!p1.getBeregningsresultatPeriodeFom().equals(p2.getBeregningsresultatPeriodeFom())) {
            return false;
        }
        if (!p1.getBeregningsresultatPeriodeTom().equals(p2.getBeregningsresultatPeriodeTom())) {
            return false;
        }
        if (p1.getDagsats() != p2.getDagsats()) {
            return false;
        }
        if (p1.getDagsatsFraBg() != p2.getDagsatsFraBg()) {
            return false;
        }
        if (!alleAndelerErLike(p1.getBeregningsresultatAndelList(), p2.getBeregningsresultatAndelList())) {
            return false;
        }
        return true;
    }

    private static boolean alleAndelerErLike(List<BeregningsresultatAndel> liste1, List<BeregningsresultatAndel> liste2) {
        if (liste1.size() != liste2.size()) {
            return false;
        }
        for (var andel1 : liste1) {
            Optional<BeregningsresultatAndel> matchendeAndel = liste2.stream()
                .filter(andel2 -> erAndelLik(andel1, andel2))
                .findFirst();
            if (matchendeAndel.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean erAndelLik(BeregningsresultatAndel andel1, BeregningsresultatAndel andel2) {
        if (!andel1.getAktivitetStatus().equals(andel2.getAktivitetStatus())) {
            return false;
        }
        if (!andel1.getArbeidsgiver().equals(andel2.getArbeidsgiver())) {
            return false;
        }
        if (!andel1.getInntektskategori().equals(andel2.getInntektskategori())) {
            return false;
        }
        if (!andel1.getArbeidsforholdType().equals(andel2.getArbeidsforholdType())) {
            return false;
        }
        if (!erBeløpLike(andel1.getUtbetalingsgrad(), andel2.getUtbetalingsgrad())) {
            return false;
        }
        if (!erBeløpLike(andel1.getStillingsprosent(), andel2.getStillingsprosent())) {
            return false;
        }
        if (andel1.getDagsats() != andel2.getDagsats()) {
            return false;
        }
        if (andel1.getDagsatsFraBg() != andel2.getDagsatsFraBg()) {
            return false;
        }
        if (andel1.erBrukerMottaker() != andel2.erBrukerMottaker()) {
            return false;
        }
        return true;
    }

    private static boolean erBeløpLike(BigDecimal beløp1, BigDecimal beløp2) {
        return beløp1 == null && beløp2 == null || beløp1 != null && beløp2 != null && beløp1.compareTo(beløp2) == 0;
    }

}
