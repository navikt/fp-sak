package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class LeggTilResultat {

    private LeggTilResultat() {
        // skjul public constructor
    }

    public static void leggTil(Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result,
                        AksjonspunktÅrsak årsak,
                        Arbeidsgiver arbeidsgiver,
                        Set<InternArbeidsforholdRef> arbeidsforholdRefs) {
        final Set<ArbeidsforholdMedÅrsak> arbeidsgiverSet = result.getOrDefault(arbeidsgiver, new HashSet<>());
        arbeidsforholdRefs.forEach(ref -> {
            Optional<ArbeidsforholdMedÅrsak> arbeidsforhold = finnArbeidsforholdMedMatchendeReferanse(arbeidsgiverSet, ref);
            if (arbeidsforhold.isPresent()) {
                arbeidsforhold.get().leggTilÅrsak(årsak);
            } else {
                arbeidsgiverSet.add(new ArbeidsforholdMedÅrsak(ref).leggTilÅrsak(årsak));
            }
        });
        result.put(arbeidsgiver, arbeidsgiverSet);
    }

    private static Optional<ArbeidsforholdMedÅrsak> finnArbeidsforholdMedMatchendeReferanse(Set<ArbeidsforholdMedÅrsak> arbeidsgiverSet, InternArbeidsforholdRef ref) {
        return arbeidsgiverSet.stream()
            .filter(it -> it.getRef().equals(ref))
            .findFirst();
    }


}
