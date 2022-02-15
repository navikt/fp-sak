package no.nav.foreldrepenger.domene.mappers.endringutleder;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.oppdateringresultat.ErTidsbegrensetArbeidsforholdEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.ToggleEndring;

class UtledErTidsbegrensetArbeidsforholdEndringer {

    private UtledErTidsbegrensetArbeidsforholdEndringer() {
        // Skjul
    }

    public static List<ErTidsbegrensetArbeidsforholdEndring> utled(BeregningsgrunnlagGrunnlagEntitet grunnlag,
                                                                   Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {

        var arbeidMedTidsbegrensetAvklaring = grunnlag.getBeregningsgrunnlag()
            .map(bg -> bg.getBeregningsgrunnlagPerioder().get(0))
            .stream()
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(a -> a.getBgAndelArbeidsforhold().isPresent() && a.getBgAndelArbeidsforhold().get().getErTidsbegrensetArbeidsforhold() != null)
            .collect(Collectors.toList());
        return arbeidMedTidsbegrensetAvklaring.stream()
            .map(andel -> utledErTidsbegrensetArbeidsforholdEndring(andel, forrigeGrunnlag))
            .collect(Collectors.toList());
    }

    private static ErTidsbegrensetArbeidsforholdEndring utledErTidsbegrensetArbeidsforholdEndring(BeregningsgrunnlagPrStatusOgAndel andel,
                                                                                                  Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {
        var forrigeVerdi = forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .map(bg -> bg.getBeregningsgrunnlagPerioder().get(0))
            .stream()
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(a -> a.getBgAndelArbeidsforhold().isPresent() && a.getBgAndelArbeidsforhold().get().equals(a.getBgAndelArbeidsforhold().get()))
            .findFirst()
            .flatMap(BeregningsgrunnlagPrStatusOgAndel::getBgAndelArbeidsforhold)
            .map(BGAndelArbeidsforhold::getErTidsbegrensetArbeidsforhold);
        ToggleEndring toggleEndring = new ToggleEndring(forrigeVerdi.orElse(null),
            andel.getBgAndelArbeidsforhold().get().getErTidsbegrensetArbeidsforhold());
        return new ErTidsbegrensetArbeidsforholdEndring(
            andel.getArbeidsgiver().orElse(null),
            andel.getArbeidsforholdRef().orElse(null),
            toggleEndring);
    }


}
