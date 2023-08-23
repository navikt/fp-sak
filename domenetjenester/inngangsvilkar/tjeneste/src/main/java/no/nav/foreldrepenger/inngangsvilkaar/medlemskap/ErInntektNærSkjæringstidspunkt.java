package no.nav.foreldrepenger.inngangsvilkaar.medlemskap;

import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;

import java.time.LocalDate;
import java.util.Collection;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;

class ErInntektNærSkjæringstidspunkt {

    private ErInntektNærSkjæringstidspunkt() {
        // skjul public constructor
    }
    static boolean erNær(Collection<Inntektspost> inntektsposter, LocalDate skjæringstidspunkt, LocalDate behandlingstidspunkt) {
        return inntektsposter.stream().anyMatch(ip -> erNær(ip, skjæringstidspunkt, behandlingstidspunkt));
    }

    static boolean erNær(Inntektspost inntektspost, LocalDate skjæringstidspunkt, LocalDate behandlingstidspunkt) {
        var inntektpåkrevdMåned = inntektPåkrevdMåned(skjæringstidspunkt, behandlingstidspunkt);
        return inntektspost.getPeriode().inkluderer(inntektpåkrevdMåned);
    }

    private static LocalDate inntektPåkrevdMåned(LocalDate skjæringstidspunkt, LocalDate behandlingstidspunkt) {
        var skjæringstidspunktMåned = skjæringstidspunkt.with(firstDayOfMonth());
        var behandlingstidspunktMåned = behandlingstidspunkt.with(firstDayOfMonth());
        if (skjæringstidspunktMåned.isEqual(behandlingstidspunktMåned)){
            return skjæringstidspunktMåned.minusMonths(1);
        }
        return skjæringstidspunktMåned;
    }
}
