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
        if (p1.getBeregningsresultatAndelList().size() != p2.getBeregningsresultatAndelList().size()) {
            return false;
        }
        if (!p1.getBeregningsresultatAndelList().containsAll(p2.getBeregningsresultatAndelList())) {
            return false;
        }
        return true;
    }
}
