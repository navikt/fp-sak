package no.nav.foreldrepenger.mottak.dokumentpersiterer;

import no.nav.vedtak.exception.TekniskException;

public final class InntektsmeldingFeil {

    private InntektsmeldingFeil() {
    }

    public static TekniskException manglendeInformasjon() {
        return new TekniskException("FP-938211", "Fant ikke informasjon om arbeidsforhold på inntektsmelding");
    }
}
