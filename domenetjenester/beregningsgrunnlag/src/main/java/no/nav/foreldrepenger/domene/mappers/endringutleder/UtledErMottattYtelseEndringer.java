package no.nav.foreldrepenger.domene.mappers.endringutleder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.FaktaAggregat;
import no.nav.foreldrepenger.domene.modell.FaktaAktør;
import no.nav.foreldrepenger.domene.modell.FaktaArbeidsforhold;
import no.nav.foreldrepenger.domene.oppdateringresultat.ErMottattYtelseEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.ToggleEndring;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class UtledErMottattYtelseEndringer {

    private UtledErMottattYtelseEndringer() {
        // Skjul
    }

    static List<ErMottattYtelseEndring> utled(FaktaAggregat fakta, Optional<FaktaAggregat> forrigeFakta) {
        List<ErMottattYtelseEndring> endringer = new ArrayList<>();
        utledFLMottarYtelseEndring(fakta, forrigeFakta).ifPresent(endringer::add);
        List<FaktaArbeidsforhold> faktaArbeidsforhold = fakta.getFaktaArbeidsforhold();
        List<FaktaArbeidsforhold> forrigeFaktaArbeidsforhold = forrigeFakta.map(FaktaAggregat::getFaktaArbeidsforhold)
            .orElse(Collections.emptyList());
        faktaArbeidsforhold.stream()
            .map(fa -> utledErMottattYtelseEndring(fa, forrigeFaktaArbeidsforhold))
            .filter(Objects::nonNull)
            .forEach(endringer::add);
        return endringer;
    }

    private static Optional<ErMottattYtelseEndring> utledFLMottarYtelseEndring(FaktaAggregat fakta, Optional<FaktaAggregat> forrigeFakta) {
        Boolean flMottarYtelse = fakta.getFaktaAktør().map(FaktaAktør::getHarFLMottattYtelseVurdering).orElse(null);
        if (flMottarYtelse != null) {
            var forrigeFlMottarYtelse = forrigeFakta.flatMap(FaktaAggregat::getFaktaAktør)
                .map(FaktaAktør::getHarFLMottattYtelseVurdering)
                .orElse(null);
            if (forrigeFlMottarYtelse == null || !forrigeFlMottarYtelse.equals(flMottarYtelse)) {
                return Optional.of(
                    ErMottattYtelseEndring.lagErMottattYtelseEndringForFrilans(new ToggleEndring(forrigeFlMottarYtelse, flMottarYtelse)));
            }
        }
        return Optional.empty();
    }

    private static ErMottattYtelseEndring utledErMottattYtelseEndring(FaktaArbeidsforhold fakta, List<FaktaArbeidsforhold> forrigeFaktaListe) {
        Optional<FaktaArbeidsforhold> forrigeFakta = finnForrigeFakta(forrigeFaktaListe, fakta.getArbeidsgiver(), fakta.getArbeidsforholdRef());
        ToggleEndring toggleEndring = utledErMottattYtelseEndring(fakta, forrigeFakta);
        if (toggleEndring != null) {
            return ErMottattYtelseEndring.lagErMottattYtelseEndringForArbeid(toggleEndring, fakta.getArbeidsgiver(),
                fakta.getArbeidsforholdRef());
        }
        return null;
    }

    private static ToggleEndring utledErMottattYtelseEndring(FaktaArbeidsforhold fakta, Optional<FaktaArbeidsforhold> forrigeFakta) {
        if (fakta.getHarMottattYtelseVurdering() != null && harEndringIMottarYtelse(
            forrigeFakta.map(FaktaArbeidsforhold::getHarMottattYtelseVurdering), fakta.getHarMottattYtelseVurdering())) {
            return initMottarYtelseEndring(forrigeFakta, fakta.getHarMottattYtelseVurdering());
        }
        return null;
    }

    private static ToggleEndring initMottarYtelseEndring(Optional<FaktaArbeidsforhold> forrigeFakta, Boolean mottarYtelse) {
        return new ToggleEndring(finnMottarYtelse(forrigeFakta), mottarYtelse);
    }

    private static Boolean finnMottarYtelse(Optional<FaktaArbeidsforhold> forrigeFakta) {
        return forrigeFakta.map(FaktaArbeidsforhold::getHarMottattYtelseVurdering).orElse(null);
    }

    private static Boolean harEndringIMottarYtelse(Optional<Boolean> forrigeMottarYtelse, Boolean mottarYtelse) {
        return forrigeMottarYtelse.map(m -> !m.equals(mottarYtelse)).orElse(true);
    }

    private static Optional<FaktaArbeidsforhold> finnForrigeFakta(List<FaktaArbeidsforhold> forrigeFaktaArbeidsforhold,
                                                                  Arbeidsgiver arbeidsgiver,
                                                                  InternArbeidsforholdRef arbeidsforholdRefDto) {
        return forrigeFaktaArbeidsforhold.stream().filter(fa -> fa.gjelderFor(arbeidsgiver, arbeidsforholdRefDto)).findFirst();
    }


}
