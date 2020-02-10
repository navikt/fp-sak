package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class IkkeTattStillingTil {
    private IkkeTattStillingTil() {
        // skjul public constructor
    }

    public static boolean vurder(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, InntektArbeidYtelseGrunnlag grunnlag) {
        final Optional<ArbeidsforholdInformasjon> informasjon = grunnlag.getArbeidsforholdInformasjon();
        if (informasjon.isPresent()) {
            final ArbeidsforholdInformasjon arbeidsforholdInformasjon = informasjon.get();
            return arbeidsforholdInformasjon.getOverstyringer()
                .stream()
                .noneMatch(ov -> ov.getArbeidsgiver().equals(arbeidsgiver)
                    && ov.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef));
        }
        return arbeidsforholdRef!=null && arbeidsforholdRef.gjelderForSpesifiktArbeidsforhold();
    }
}
