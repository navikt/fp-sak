package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class OmfordelRevurderingsandelerUtenReferanse {

    private OmfordelRevurderingsandelerUtenReferanse() {
        // SKjuler default
    }

    public static List<EndringIBeregningsresultat> omfordel(BRNøkkelMedAndeler revurderingNøkkelMedAndeler, BRNøkkelMedAndeler originalNøkkelMedAndeler) {
        var revurderingReferanser = revurderingNøkkelMedAndeler.getAlleReferanserForDenneNøkkelen();
        List<EndringIBeregningsresultat> list = new ArrayList<>();
        var andelerMedRefSomIkkeFinnesIRevurdering = originalNøkkelMedAndeler.getAlleAndelerMedRefSomIkkeFinnesIListe(revurderingReferanser);

        var brukersAndelUtenreferanse = revurderingNøkkelMedAndeler.getBrukersAndelUtenreferanse();
        var arbeidsgiversAndelUtenReferanse = revurderingNøkkelMedAndeler.getArbeidsgiversAndelUtenReferanse();

        if (brukersAndelUtenreferanse.isPresent()) {
            var omberegnetDagsats = beregnDagsatsBrukerAndelUtenReferanse(originalNøkkelMedAndeler, andelerMedRefSomIkkeFinnesIRevurdering ,
                brukersAndelUtenreferanse.get(), arbeidsgiversAndelUtenReferanse);
            if (erDagsatsEndret(brukersAndelUtenreferanse.get(), omberegnetDagsats)) {
                var endring = new EndringIBeregningsresultat(brukersAndelUtenreferanse.get(), omberegnetDagsats);
                list.add(endring);
            }
        }
        if (arbeidsgiversAndelUtenReferanse.isPresent()) {
            var omberegnetDagsats = beregnDagsatsAGUtenReferanse(originalNøkkelMedAndeler, andelerMedRefSomIkkeFinnesIRevurdering ,
                arbeidsgiversAndelUtenReferanse.get(), brukersAndelUtenreferanse);
            if (erDagsatsEndret(arbeidsgiversAndelUtenReferanse.get(), omberegnetDagsats)) {
                var endring = new EndringIBeregningsresultat(arbeidsgiversAndelUtenReferanse.get(), omberegnetDagsats);
                list.add(endring);
            }
        }
        return list;
    }

    private static int beregnDagsatsAGUtenReferanse(BRNøkkelMedAndeler originalNøkkelMedAndeler,
                                                    List<BeregningsresultatAndel> alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering,
                                                    BeregningsresultatAndel arbeidsgiversAndelUtenReferanse,
                                                    Optional<BeregningsresultatAndel> brukersAndelUtenreferanse) {
        var originalDagsatsBrukersAndelUtenMatchendeRef = alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering.stream()
            .filter(BeregningsresultatAndel::erBrukerMottaker)
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();
        int originalDagsatsBrukersAndelUtenRef = originalNøkkelMedAndeler.getBrukersAndelUtenreferanse()
            .map(BeregningsresultatAndel::getDagsats)
            .orElse(0);
        var originalDagsatsBrukerTotal = originalDagsatsBrukersAndelUtenRef + originalDagsatsBrukersAndelUtenMatchendeRef;
        int revurderingBrukersDagsats = brukersAndelUtenreferanse
            .map(BeregningsresultatAndel::getDagsats)
            .orElse(0);
        return OmfordelDagsats.beregnDagsatsArbeidsgiver(arbeidsgiversAndelUtenReferanse.getDagsats(), revurderingBrukersDagsats, originalDagsatsBrukerTotal);


    }
    private static int beregnDagsatsBrukerAndelUtenReferanse(BRNøkkelMedAndeler originalNøkkelMedAndeler,
                                                             List<BeregningsresultatAndel> alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering,
                                                             BeregningsresultatAndel brukersAndelUtenreferanse,
                                                             Optional<BeregningsresultatAndel> arbeidsgiversAndelUtenReferanse) {
        var totalOriginalBrukersDagsats = finnOriginalBrukerDagsats(originalNøkkelMedAndeler, alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering);
        var revurderingBrukerDagsats = brukersAndelUtenreferanse.getDagsats();
        int revurderingDagsatsArbeidsgiver = arbeidsgiversAndelUtenReferanse
            .map(BeregningsresultatAndel::getDagsats)
            .orElse(0);
        return OmfordelDagsats.beregnDagsatsBruker(revurderingBrukerDagsats, revurderingDagsatsArbeidsgiver, totalOriginalBrukersDagsats);
    }

    private static int finnOriginalBrukerDagsats(BRNøkkelMedAndeler originalNøkkelMedAndeler, List<BeregningsresultatAndel> alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering) {
        int originalDagsatsAndelUtenReferanse = originalNøkkelMedAndeler.getBrukersAndelUtenreferanse()
            .map(BeregningsresultatAndel::getDagsats)
            .orElse(0);
        var originaleBrukersAndelSomIkkeMatcherRevurderingAndeler = alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering.stream()
            .filter(BeregningsresultatAndel::erBrukerMottaker)
            .toList();
        var originalDagsatsAndelerUtenMatchendeRef = originaleBrukersAndelSomIkkeMatcherRevurderingAndeler.stream().mapToInt(BeregningsresultatAndel::getDagsats).sum();
        return originalDagsatsAndelUtenReferanse + originalDagsatsAndelerUtenMatchendeRef;
    }

    private static boolean erDagsatsEndret(BeregningsresultatAndel andel, int omberegnetDagsats) {
        return omberegnetDagsats != andel.getDagsats();
    }


}
