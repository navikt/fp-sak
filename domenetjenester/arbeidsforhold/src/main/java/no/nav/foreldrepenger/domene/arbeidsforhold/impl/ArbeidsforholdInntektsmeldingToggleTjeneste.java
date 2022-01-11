package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

public class ArbeidsforholdInntektsmeldingToggleTjeneste {

    private ArbeidsforholdInntektsmeldingToggleTjeneste() {
        // Skjuler default
    }

    public static boolean erTogglePå() {
        // Skrur toggle helt av i starten for å ikke trenge endringer i autotest før vi er trygge på at dette er løsningen vi går for
        return false;
//        return !Environment.current().isProd();
    }
}
