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

        Optional<BeregningsresultatAndel> brukersAndelUtenreferanse = revurderingNøkkelMedAndeler.getBrukersAndelUtenreferanse();
        Optional<BeregningsresultatAndel> arbeidsgiversAndelUtenReferanse = revurderingNøkkelMedAndeler.getArbeidsgiversAndelUtenReferanse();

        if (brukersAndelUtenreferanse.isPresent()) {
            int omberegnetDagsats = beregnDagsatsBrukerAndelUtenReferanse(originalNøkkelMedAndeler, andelerMedRefSomIkkeFinnesIRevurdering ,
                brukersAndelUtenreferanse.get(), arbeidsgiversAndelUtenReferanse);
            if (erDagsatsEndret(brukersAndelUtenreferanse.get(), omberegnetDagsats)) {
                EndringIBeregningsresultat endring = new EndringIBeregningsresultat(brukersAndelUtenreferanse.get(), omberegnetDagsats);
                list.add(endring);
            }
        }
        if (arbeidsgiversAndelUtenReferanse.isPresent()) {
            int omberegnetDagsats = beregnDagsatsAGUtenReferanse(originalNøkkelMedAndeler, andelerMedRefSomIkkeFinnesIRevurdering ,
                arbeidsgiversAndelUtenReferanse.get(), brukersAndelUtenreferanse);
            if (erDagsatsEndret(arbeidsgiversAndelUtenReferanse.get(), omberegnetDagsats)) {
                EndringIBeregningsresultat endring = new EndringIBeregningsresultat(arbeidsgiversAndelUtenReferanse.get(), omberegnetDagsats);
                list.add(endring);
            }
        }
        return list;
    }

    private static int beregnDagsatsAGUtenReferanse(BRNøkkelMedAndeler originalNøkkelMedAndeler,
                                                    List<BeregningsresultatAndel> alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering,
                                                    BeregningsresultatAndel arbeidsgiversAndelUtenReferanse,
                                                    Optional<BeregningsresultatAndel> brukersAndelUtenreferanse) {
        int originalDagsatsBrukersAndelUtenMatchendeRef = alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering.stream()
            .filter(BeregningsresultatAndel::erBrukerMottaker)
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();
        int originalDagsatsBrukersAndelUtenRef = originalNøkkelMedAndeler.getBrukersAndelUtenreferanse()
            .map(BeregningsresultatAndel::getDagsats)
            .orElse(0);
        int originalDagsatsBrukerTotal = originalDagsatsBrukersAndelUtenRef + originalDagsatsBrukersAndelUtenMatchendeRef;
        int revurderingBrukersDagsats = brukersAndelUtenreferanse
            .map(BeregningsresultatAndel::getDagsats)
            .orElse(0);
        return OmfordelDagsats.beregnDagsatsArbeidsgiver(arbeidsgiversAndelUtenReferanse.getDagsats(), revurderingBrukersDagsats, originalDagsatsBrukerTotal);


    }
    private static int beregnDagsatsBrukerAndelUtenReferanse(BRNøkkelMedAndeler originalNøkkelMedAndeler,
                                                             List<BeregningsresultatAndel> alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering,
                                                             BeregningsresultatAndel brukersAndelUtenreferanse,
                                                             Optional<BeregningsresultatAndel> arbeidsgiversAndelUtenReferanse) {
        int totalOriginalBrukersDagsats = finnOriginalBrukerDagsats(originalNøkkelMedAndeler, alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering);
        int revurderingBrukerDagsats = brukersAndelUtenreferanse.getDagsats();
        int revurderingDagsatsArbeidsgiver = arbeidsgiversAndelUtenReferanse
            .map(BeregningsresultatAndel::getDagsats)
            .orElse(0);
        return OmfordelDagsats.beregnDagsatsBruker(revurderingBrukerDagsats, revurderingDagsatsArbeidsgiver, totalOriginalBrukersDagsats);
    }

    private static int finnOriginalBrukerDagsats(BRNøkkelMedAndeler originalNøkkelMedAndeler, List<BeregningsresultatAndel> alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering) {
        int originalDagsatsAndelUtenReferanse = originalNøkkelMedAndeler.getBrukersAndelUtenreferanse()
            .map(BeregningsresultatAndel::getDagsats)
            .orElse(0);
        List<BeregningsresultatAndel> originaleBrukersAndelSomIkkeMatcherRevurderingAndeler = alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering.stream()
            .filter(BeregningsresultatAndel::erBrukerMottaker)
            .collect(Collectors.toList());
        int originalDagsatsAndelerUtenMatchendeRef = originaleBrukersAndelSomIkkeMatcherRevurderingAndeler.stream().mapToInt(BeregningsresultatAndel::getDagsats).sum();
        return originalDagsatsAndelUtenReferanse + originalDagsatsAndelerUtenMatchendeRef;
    }

    private static boolean erDagsatsEndret(BeregningsresultatAndel andel, int omberegnetDagsats) {
        return omberegnetDagsats != andel.getDagsats();
    }


}
