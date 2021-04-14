package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak.ENDRING_I_ARBEIDSFORHOLDS_ID;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class EndringIArbeidsforholdId {
    private static final Logger LOG = LoggerFactory.getLogger(EndringIArbeidsforholdId.class);

    private EndringIArbeidsforholdId() {
        // skjul public constructor
    }

    public static void vurderMedÅrsak(Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result,
            Map.Entry<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyInntektsmelding,
            Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> eksisterendeIM,
            InntektArbeidYtelseGrunnlag grunnlag,
            Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> yrkesaktiviteterPerArbeidsgiver) {
        final var arbeidsgiver = nyInntektsmelding.getKey();
        var påkrevdRefSet = yrkesaktiviteterPerArbeidsgiver.getOrDefault(arbeidsgiver, Collections.emptySet());
        final var nyRefSet = nyInntektsmelding.getValue();
        final var eksisterendeRefSet = eksisterendeIM.getOrDefault(arbeidsgiver, Collections.emptySet());

        if (!påkrevdRefSet.equals(nyRefSet) && !endretTilIkkeSpesifiktArbeidsforhold(nyRefSet)) {
            var manglerIM = påkrevdRefSet.stream()
                    .filter(ref -> !nyRefSet.contains(ref))
                    .filter(it -> IkkeTattStillingTil.vurder(arbeidsgiver, it, grunnlag))
                    .collect(Collectors.toSet());
            var uventetIM = nyRefSet.stream()
                    .filter(ref -> !påkrevdRefSet.contains(ref))
                    .collect(Collectors.toSet());

            Set<InternArbeidsforholdRef> vurderIM = new HashSet<>(manglerIM);
            vurderIM.addAll(uventetIM);

            if (!vurderIM.isEmpty()) {
                LeggTilResultat.leggTil(result, ENDRING_I_ARBEIDSFORHOLDS_ID, arbeidsgiver, vurderIM);
                LOG.info("Endring i arbeidsforholdsId: arbeidsgiver={}, fra arbeidsforholdRef={} til arbeidsforholdRef={}", arbeidsgiver,
                        eksisterendeRefSet, nyRefSet);
            }
        }
    }

    private static boolean endretTilIkkeSpesifiktArbeidsforhold(Set<InternArbeidsforholdRef> nyRefSet) {
        return (nyRefSet.size() == 1) && !nyRefSet.iterator().next().gjelderForSpesifiktArbeidsforhold();
    }
}
