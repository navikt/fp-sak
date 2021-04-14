package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetOgArbeidsgiverNøkkel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;

class FinnEndringerIUtbetaltYtelse {
    private FinnEndringerIUtbetaltYtelse() {
        // skjul public constructor
    }

    static List<EndringIBeregningsresultat> finnEndringer(List<BeregningsresultatAndel> originaleAndeler, List<BeregningsresultatAndel> revurderingAndeler) {

        var originaleAndelerSortertPåNøkkel = MapAndelerSortertPåNøkkel.map(originaleAndeler);
        var revurderingAndelerSortertPåNøkkel = MapAndelerSortertPåNøkkel.map(revurderingAndeler);

        List<EndringIBeregningsresultat> list = new ArrayList<>();

        for (var revurderingNøkkelMedAndeler : revurderingAndelerSortertPåNøkkel) {
            if (revurderingNøkkelMedAndeler.erArbeidstaker()) {
                list.addAll(finnEndringerForAndelerSomErAT(originaleAndelerSortertPåNøkkel, revurderingNøkkelMedAndeler));
            }
        }
        return list;
    }

    private static List<EndringIBeregningsresultat> finnEndringerForAndelerSomErAT(List<BRNøkkelMedAndeler> originaleAndelerSortertPåNøkkel, BRNøkkelMedAndeler revurderingNøkkelMedAndeler) {
        var originaleAndelerMedSammeNøkkel = finnSammenligningsandelMedSammeNøkkel(revurderingNøkkelMedAndeler.getNøkkel(), originaleAndelerSortertPåNøkkel);
        // Nøkkelen er ny, og vi har ingen andeler å overføre fra
        return originaleAndelerMedSammeNøkkel.map(andelerMedSammeNøkkel -> Omfordelingstjeneste.omfordel(andelerMedSammeNøkkel, revurderingNøkkelMedAndeler)).orElse(Collections.emptyList());
    }

    private static Optional<BRNøkkelMedAndeler> finnSammenligningsandelMedSammeNøkkel(AktivitetOgArbeidsgiverNøkkel nøkkel, List<BRNøkkelMedAndeler> liste) {
        var matchendeNøkler = liste.stream()
            .filter(a -> Objects.equals(a.getNøkkel(), nøkkel))
            .collect(Collectors.toList());
        if (matchendeNøkler.size() > 1) {
            throw new IllegalStateException("Forventet å ikke finne mer enn en matchende nøkkel i liste for nøkkel " + nøkkel + " men fant " + matchendeNøkler.size());
        }
        return matchendeNøkler.stream().findFirst();
    }

}
