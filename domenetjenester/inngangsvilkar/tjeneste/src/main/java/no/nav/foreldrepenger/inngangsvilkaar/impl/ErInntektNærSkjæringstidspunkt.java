package no.nav.foreldrepenger.inngangsvilkaar.impl;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;

import java.time.LocalDate;
import java.util.Collection;

import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;

class ErInntektNærSkjæringstidspunkt {

    private ErInntektNærSkjæringstidspunkt() {
        // skjul public constructor
    }
    static boolean erNær(Collection<Inntektspost> inntektsposter, LocalDate skjæringstidspunkt, LocalDate behandlingstidspunkt) {
        return inntektsposter.stream().anyMatch(ip -> erNær(ip, skjæringstidspunkt, behandlingstidspunkt));
    }
    
    static boolean erNær(Inntektspost inntektspost, LocalDate skjæringstidspunkt, LocalDate behandlingstidspunkt) {
        LocalDate inntektpåkrevdMåned = inntektPåkrevdMåned(skjæringstidspunkt, behandlingstidspunkt);
        return inntektspost.getPeriode().inkluderer(inntektpåkrevdMåned);
    }

    private static LocalDate inntektPåkrevdMåned(LocalDate skjæringstidspunkt, LocalDate behandlingstidspunkt) {
        LocalDate skjæringstidspunktMåned = skjæringstidspunkt.with(firstDayOfMonth());
        LocalDate behandlingstidspunktMåned = behandlingstidspunkt.with(firstDayOfMonth());
        if (skjæringstidspunktMåned.isEqual(behandlingstidspunktMåned)){
            return skjæringstidspunktMåned.minusMonths(1);
        }
        return skjæringstidspunktMåned;
    }
}
