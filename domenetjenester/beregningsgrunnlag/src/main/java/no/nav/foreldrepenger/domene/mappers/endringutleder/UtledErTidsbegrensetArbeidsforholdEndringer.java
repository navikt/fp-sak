package no.nav.foreldrepenger.domene.mappers.endringutleder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.FaktaAggregat;
import no.nav.foreldrepenger.domene.modell.FaktaArbeidsforhold;
import no.nav.foreldrepenger.domene.oppdateringresultat.ErTidsbegrensetArbeidsforholdEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.ToggleEndring;

class UtledErTidsbegrensetArbeidsforholdEndringer {

    private UtledErTidsbegrensetArbeidsforholdEndringer() {
        // Skjul
    }

    public static List<ErTidsbegrensetArbeidsforholdEndring> utled(FaktaAggregat fakta, Optional<FaktaAggregat> forrigeFakta) {

        List<FaktaArbeidsforhold> arbeidMedTidsbegrensetAvklaring = fakta.getFaktaArbeidsforhold()
            .stream()
            .filter(fa -> fa.getErTidsbegrensetVurdering() != null)
            .collect(Collectors.toList());
        List<FaktaArbeidsforhold> forrigeArbeidFakta = forrigeFakta.map(FaktaAggregat::getFaktaArbeidsforhold).orElse(Collections.emptyList());
        return arbeidMedTidsbegrensetAvklaring.stream()
            .map(fa -> utledErTidsbegrensetArbeidsforholdEndring(fa, forrigeArbeidFakta))
            .collect(Collectors.toList());
    }

    private static ErTidsbegrensetArbeidsforholdEndring utledErTidsbegrensetArbeidsforholdEndring(FaktaArbeidsforhold faktaArbeidsforhold,
                                                                                                  List<FaktaArbeidsforhold> forrigeFaktaListe) {
        Optional<FaktaArbeidsforhold> forrigeFakta = forrigeFaktaListe.stream()
            .filter(a -> a.gjelderFor(faktaArbeidsforhold.getArbeidsgiver(), faktaArbeidsforhold.getArbeidsforholdRef()))
            .findFirst();
        ToggleEndring toggleEndring = utledErTidsbegrensetEndring(faktaArbeidsforhold, forrigeFakta);
        Arbeidsgiver arbeidsgiver = faktaArbeidsforhold.getArbeidsgiver();
        return new ErTidsbegrensetArbeidsforholdEndring(arbeidsgiver, faktaArbeidsforhold.getArbeidsforholdRef(), toggleEndring);
    }

    private static ToggleEndring utledErTidsbegrensetEndring(FaktaArbeidsforhold fakta, Optional<FaktaArbeidsforhold> forrigeFakta) {
        Boolean fraVerdi = forrigeFakta.map(FaktaArbeidsforhold::getErTidsbegrensetVurdering).orElse(null);
        Boolean tilVerdi = fakta.getErTidsbegrensetVurdering();
        return new ToggleEndring(fraVerdi, tilVerdi);
    }


}
