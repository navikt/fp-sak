package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class HentBekreftetPermisjon {

    private HentBekreftetPermisjon() {
        // skjul public constructor
    }

    public static Optional<BekreftetPermisjon> hent(InntektArbeidYtelseGrunnlag grunnlag, Yrkesaktivitet yrkesaktivitet) {
        return hent(grunnlag, yrkesaktivitet.getArbeidsgiver(), yrkesaktivitet.getArbeidsforholdRef());
    }

    public static Optional<BekreftetPermisjon> hent(InntektArbeidYtelseGrunnlag grunnlag, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        List<ArbeidsforholdOverstyring> overstyringer = hentAlleOverstyringer(grunnlag);
        Optional<ArbeidsforholdOverstyring> overstyring = finnOverstyringSomMatcherArbeidsforhold(arbeidsgiver, arbeidsforholdRef, overstyringer);
        return overstyring.flatMap(ArbeidsforholdOverstyring::getBekreftetPermisjon);
    }

    private static Optional<ArbeidsforholdOverstyring> finnOverstyringSomMatcherArbeidsforhold(Arbeidsgiver arbeidsgiver,
                                                                                                      InternArbeidsforholdRef arbeidsforholdRef,
                                                                                                      List<ArbeidsforholdOverstyring> overstyringer) {
        return overstyringer.stream()
            .filter(over -> Objects.equals(arbeidsgiver, over.getArbeidsgiver()) && arbeidsforholdRef.gjelderFor(over.getArbeidsforholdRef()))
            .findFirst();
    }

    private static List<ArbeidsforholdOverstyring> hentAlleOverstyringer(InntektArbeidYtelseGrunnlag grunnlag) {
        return grunnlag.getArbeidsforholdOverstyringer();
    }

}
