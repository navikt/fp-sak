package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import no.nav.foreldrepenger.konfig.Environment;

public class ArbeidsforholdInntektsmeldingToggleTjeneste {

    private ArbeidsforholdInntektsmeldingToggleTjeneste() {
        // Skjuler default
    }

    public static boolean erTogglePå() {
        return !Environment.current().isProd();
    }
}
