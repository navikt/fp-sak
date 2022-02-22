package no.nav.foreldrepenger.domene.mappers.endringutleder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningsgrunnlagPeriodeEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

class UtledEndringIPeriodeFraEntitet {

    private UtledEndringIPeriodeFraEntitet() {
        // skjul
    }

    public static Optional<BeregningsgrunnlagPeriodeEndring> utled(BeregningsgrunnlagPeriode periode,
                                                                   BeregningsgrunnlagPeriode periodeFraSteg,
                                                                   Optional<BeregningsgrunnlagPeriode> forrigePeriode) {
        var andeler = periode.getBeregningsgrunnlagPrStatusOgAndelList();
        var andelerFraSteg = periodeFraSteg.getBeregningsgrunnlagPrStatusOgAndelList();

        var forrigeAndeler = forrigePeriode.map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPrStatusOgAndelList).orElse(Collections.emptyList());
        var periodeEndring = new BeregningsgrunnlagPeriodeEndring(
                utledAndelEndringer(andeler, andelerFraSteg, forrigeAndeler),
                DatoIntervallEntitet.fraOgMedTilOgMed(periode.getBeregningsgrunnlagPeriodeFom(), periode.getBeregningsgrunnlagPeriodeTom())
                );
        if (periodeEndring.getBeregningsgrunnlagPrStatusOgAndelEndringer().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(periodeEndring);
    }

    private static List<BeregningsgrunnlagPrStatusOgAndelEndring> utledAndelEndringer(List<BeregningsgrunnlagPrStatusOgAndel> andeler, List<BeregningsgrunnlagPrStatusOgAndel> andelerFraSteg, List<BeregningsgrunnlagPrStatusOgAndel> forrigeAndeler) {
        return andeler.stream()
                .map(a -> {
                    var forrigeAndel = finnAndel(forrigeAndeler, a);
                    var andelFraSteg = finnAndel(andelerFraSteg, a);
                    return UtledEndringIAndel.utled(a, andelFraSteg, forrigeAndel);
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private static Optional<BeregningsgrunnlagPrStatusOgAndel> finnAndel(List<BeregningsgrunnlagPrStatusOgAndel> forrigeAndeler, BeregningsgrunnlagPrStatusOgAndel andel) {
        if (andel.getAktivitetStatus().erArbeidstaker()) {
            return forrigeAndeler.stream().filter(a -> a.equals(andel)).findFirst();
        }
        return forrigeAndeler.stream()
                .filter(a -> a.getAktivitetStatus().equals(andel.getAktivitetStatus())).findFirst();
    }
}
