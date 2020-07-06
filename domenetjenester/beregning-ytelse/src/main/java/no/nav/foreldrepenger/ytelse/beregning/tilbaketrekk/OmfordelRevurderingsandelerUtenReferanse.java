package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class OmfordelRevurderingsandelerUtenReferanse {

    private OmfordelRevurderingsandelerUtenReferanse() {
        // SKjuler default
    }

    public static List<EndringIBeregningsresultat> omfordel(BRNøkkelMedAndeler revurderingNøkkelMedAndeler, BRNøkkelMedAndeler originalNøkkelMedAndeler) {
        List<InternArbeidsforholdRef> revurderingReferanser = revurderingNøkkelMedAndeler.getAlleReferanserForDenneNøkkelen();
        List<EndringIBeregningsresultat> list = new ArrayList<>();
        List<BeregningsresultatAndel> andelerMedRefSomIkkeFinnesIRevurdering = originalNøkkelMedAndeler.getAlleAndelerMedRefSomIkkeFinnesIListe(revurderingReferanser);

        List<BeregningsresultatAndel> brukersAndelUtenreferanse = revurderingNøkkelMedAndeler.getAlleBrukersAndelerUtenReferanse();
        List<BeregningsresultatAndel> arbeidsgiversAndelUtenReferanse = revurderingNøkkelMedAndeler.getAlleArbeidsgiversAndelerUtenReferanse();

        if (!brukersAndelUtenreferanse.isEmpty()) {
            int omberegnetDagsats = beregnDagsatsBrukerAndelUtenReferanse(originalNøkkelMedAndeler, andelerMedRefSomIkkeFinnesIRevurdering,
                brukersAndelUtenreferanse, arbeidsgiversAndelUtenReferanse);
            if (erDagsatsEndret(brukersAndelUtenreferanse, omberegnetDagsats)) {
                list.addAll(brukersAndelUtenreferanse.stream()
                    .map(andel -> new EndringIBeregningsresultat(andel, omberegnetDagsats))
                    .collect(Collectors.toList()));
            }
        }
        if (!arbeidsgiversAndelUtenReferanse.isEmpty()) {
            int omberegnetDagsats = beregnDagsatsAGUtenReferanse(originalNøkkelMedAndeler, andelerMedRefSomIkkeFinnesIRevurdering ,
                arbeidsgiversAndelUtenReferanse, brukersAndelUtenreferanse);
            if (erDagsatsEndret(arbeidsgiversAndelUtenReferanse, omberegnetDagsats)) {
                list.addAll(arbeidsgiversAndelUtenReferanse.stream()
                    .map(andel -> new EndringIBeregningsresultat(andel, omberegnetDagsats))
                    .collect(Collectors.toList()));
            }
        }
        return list;
    }

    private static int beregnDagsatsAGUtenReferanse(BRNøkkelMedAndeler originalNøkkelMedAndeler,
                                                    List<BeregningsresultatAndel> alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering,
                                                    List<BeregningsresultatAndel> arbeidsgiversAndelUtenReferanse,
                                                    List<BeregningsresultatAndel> brukersAndelUtenreferanse) {
        int originalDagsatsBrukersAndelUtenMatchendeRef = alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering.stream()
            .filter(BeregningsresultatAndel::erBrukerMottaker)
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();
        int originalDagsatsBrukersAndelUtenRef = originalNøkkelMedAndeler.getAlleBrukersAndelerUtenReferanse().stream()
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();
        int originalDagsatsBrukerTotal = originalDagsatsBrukersAndelUtenRef + originalDagsatsBrukersAndelUtenMatchendeRef;
        int revurderingBrukersDagsats = brukersAndelUtenreferanse.stream().mapToInt(BeregningsresultatAndel::getDagsats).sum();
        int revurderingArbeidsgiversDagsats = arbeidsgiversAndelUtenReferanse.stream().mapToInt(BeregningsresultatAndel::getDagsats).sum();
        return OmfordelDagsats.beregnDagsatsArbeidsgiver(revurderingArbeidsgiversDagsats, revurderingBrukersDagsats, originalDagsatsBrukerTotal);


    }
    private static int beregnDagsatsBrukerAndelUtenReferanse(BRNøkkelMedAndeler originalNøkkelMedAndeler,
                                                             List<BeregningsresultatAndel> alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering,
                                                             List<BeregningsresultatAndel> brukersAndelUtenreferanse,
                                                             List<BeregningsresultatAndel> arbeidsgiversAndelUtenReferanse) {
        int totalOriginalBrukersDagsats = finnOriginalBrukerDagsats(originalNøkkelMedAndeler, alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering);
        int revurderingBrukerDagsats = brukersAndelUtenreferanse.stream().mapToInt(BeregningsresultatAndel::getDagsats).sum();
        int revurderingDagsatsArbeidsgiver = arbeidsgiversAndelUtenReferanse.stream().mapToInt(BeregningsresultatAndel::getDagsats).sum();
        return OmfordelDagsats.beregnDagsatsBruker(revurderingBrukerDagsats, revurderingDagsatsArbeidsgiver, totalOriginalBrukersDagsats);
    }

    private static int finnOriginalBrukerDagsats(BRNøkkelMedAndeler originalNøkkelMedAndeler, List<BeregningsresultatAndel> alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering) {
        int originalDagsatsAndelUtenReferanse = originalNøkkelMedAndeler.getAlleBrukersAndelerUtenReferanse().stream()
            .mapToInt(BeregningsresultatAndel::getDagsats).sum();
        List<BeregningsresultatAndel> originaleBrukersAndelSomIkkeMatcherRevurderingAndeler = alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering.stream()
            .filter(BeregningsresultatAndel::erBrukerMottaker)
            .collect(Collectors.toList());
        int originalDagsatsAndelerUtenMatchendeRef = originaleBrukersAndelSomIkkeMatcherRevurderingAndeler.stream().mapToInt(BeregningsresultatAndel::getDagsats).sum();
        return originalDagsatsAndelUtenReferanse + originalDagsatsAndelerUtenMatchendeRef;
    }

    private static boolean erDagsatsEndret(List<BeregningsresultatAndel> andeler, int omberegnetDagsats) {
        return omberegnetDagsats != andeler.stream().mapToInt(BeregningsresultatAndel::getDagsats).sum();
    }


}
