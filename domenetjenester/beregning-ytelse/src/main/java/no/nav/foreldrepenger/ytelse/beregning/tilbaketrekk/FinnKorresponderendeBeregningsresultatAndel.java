package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FinnKorresponderendeBeregningsresultatAndel {
    private FinnKorresponderendeBeregningsresultatAndel() {
        // skjul public constructor
    }

    public static Optional<BeregningsresultatAndel> finn(List<BeregningsresultatAndel> haystack, BeregningsresultatAndel needle, boolean erBrukerMottaker) {
        var forrigeAndelAktivitetsnøkkel = needle.getAktivitetOgArbeidsforholdNøkkel();
        var korresponderendeAndeler = haystack.stream()
            .filter(andel -> andel.erBrukerMottaker() == erBrukerMottaker)
            .filter(andel -> Objects.equals(andel.getAktivitetOgArbeidsforholdNøkkel(), forrigeAndelAktivitetsnøkkel))
            .toList();
        if (korresponderendeAndeler.size() > 1) {
            throw new IllegalArgumentException("Forventet å finne maks en korresponderende BeregningsresultatAndel " + forrigeAndelAktivitetsnøkkel
                + ". Antall matchende aktiviteter var " + korresponderendeAndeler.size());
        }
        return korresponderendeAndeler.stream().findFirst();
    }
}
