package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import no.nav.foreldrepenger.konfig.Environment;

/**
 * Toggle for 5085, se TFP-2584
 */
public class ArbeidsforholdInntektsmeldingToggleTjeneste {

    private ArbeidsforholdInntektsmeldingToggleTjeneste() {
        // Skjuler default
    }

    public static boolean erTogglePå() {
        return !Environment.current().isProd();
    }
}
