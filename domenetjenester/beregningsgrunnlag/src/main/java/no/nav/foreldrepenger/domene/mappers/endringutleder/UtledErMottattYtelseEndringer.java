package no.nav.foreldrepenger.domene.mappers.endringutleder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.oppdateringresultat.ErMottattYtelseEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.ToggleEndring;

;

class UtledErMottattYtelseEndringer {

    private UtledErMottattYtelseEndringer() {
        // Skjul
    }

    static List<ErMottattYtelseEndring> utled(BeregningsgrunnlagGrunnlagEntitet grunnlag,
                                              Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {
        List<ErMottattYtelseEndring> endringer = new ArrayList<>();
        utledFLMottarYtelseEndring(grunnlag, forrigeGrunnlag).ifPresent(endringer::add);

        grunnlag.getBeregningsgrunnlag()
            .map(bg -> bg.getBeregningsgrunnlagPerioder().get(0))
            .stream()
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(a -> a.getArbeidsgiver().isPresent() && a.mottarYtelse().isPresent())
            .map(a -> utledErMottattYtelseEndring(a, forrigeGrunnlag))
            .filter(Objects::nonNull)
            .forEach(endringer::add);
        return endringer;
    }

    private static Optional<ErMottattYtelseEndring> utledFLMottarYtelseEndring(BeregningsgrunnlagGrunnlagEntitet grunnlag,
                                                                               Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {
        Boolean flMottarYtelse = getFlMottarYtelse(grunnlag);
        if (flMottarYtelse != null) {
            var forrigeFlMottarYtelse = forrigeGrunnlag.map(UtledErMottattYtelseEndringer::getFlMottarYtelse).orElse(null);
            if (forrigeFlMottarYtelse == null || !forrigeFlMottarYtelse.equals(flMottarYtelse)) {
                return Optional.of(
                    ErMottattYtelseEndring.lagErMottattYtelseEndringForFrilans(new ToggleEndring(forrigeFlMottarYtelse, flMottarYtelse)));
            }
        }
        return Optional.empty();
    }

    private static Boolean getFlMottarYtelse(BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        return grunnlag.getBeregningsgrunnlag()
            .stream()
            .flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(a -> a.getAktivitetStatus().erFrilanser() && a.mottarYtelse().isPresent())
            .findFirst()
            .map(a -> a.mottarYtelse().get())
            .orElse(null);
    }

    private static ErMottattYtelseEndring utledErMottattYtelseEndring(BeregningsgrunnlagPrStatusOgAndel andel,
                                                                      Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {
        var forrigeMottarYtelse = forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .map(bg -> bg.getBeregningsgrunnlagPerioder().get(0))
            .stream()
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(a -> a.getArbeidsgiver().isPresent() && a.getArbeidsgiver().get().equals(a.getArbeidsgiver().get()))
            .filter(a -> a.mottarYtelse().isPresent())
            .findFirst()
            .map(a -> a.mottarYtelse().get())
            .orElse(null);

        ToggleEndring toggleEndring = new ToggleEndring(forrigeMottarYtelse, andel.mottarYtelse().orElse(null));
        return ErMottattYtelseEndring.lagErMottattYtelseEndringForArbeid(toggleEndring,
            andel.getArbeidsgiver().orElse(null),
            andel.getArbeidsforholdRef().orElse(null));
    }


}
