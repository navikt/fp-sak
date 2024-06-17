package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;

public final class Omfordelingstjeneste {

    private Omfordelingstjeneste() {
        // Skjuler default
    }

    public static List<EndringIBeregningsresultat> omfordel(BRNøkkelMedAndeler originalNøkkelMedAndeler,
                                                            BRNøkkelMedAndeler revurderingNøkkelMedAndeler) {

        List<EndringIBeregningsresultat> list = new ArrayList<>();


        // Spesialtilfellet som lar oss omfordele uten å sjekke på matchende arbeidsforoholdId'er. Se https://jira.adeo.no/browse/TFP-2260
        if (kanOmfordeleUtenÅSjekkeArbeidsforholdId(revurderingNøkkelMedAndeler, originalNøkkelMedAndeler)) {
            return OmfordelRevurderingsandelerSomHarFåttRef.omfordel(revurderingNøkkelMedAndeler, originalNøkkelMedAndeler);
        }

        // Omfordeler alle revurderingsandeler som ikke har en referanse og som ikke matcher scenario over
        list.addAll(OmfordelRevurderingsandelerUtenReferanse.omfordel(revurderingNøkkelMedAndeler, originalNøkkelMedAndeler));

        // Omfordeler alle resterende andeler
        list.addAll(OmfordelRevurderingsandelerMedReferanser.omfordel(revurderingNøkkelMedAndeler, originalNøkkelMedAndeler));

        return list;
    }

    private static boolean kanOmfordeleUtenÅSjekkeArbeidsforholdId(BRNøkkelMedAndeler revurderingNøkkelMedAndeler,
                                                                   BRNøkkelMedAndeler originalNøkkelMedAndeler) {
        var revurderingAndeler = revurderingNøkkelMedAndeler.getAndelerTilknyttetNøkkel();
        var originaleAndeler = originalNøkkelMedAndeler.getAndelerTilknyttetNøkkel();

        var revurderingHarMaksEttSettAndeler = nøkkelHarMaksEtSettMedAndeler(revurderingAndeler);
        var originalbehandlingHarMaksEttSettAndeler = nøkkelHarMaksEtSettMedAndeler(originaleAndeler);

        var ingenOriginaleAndelerHarReferanse = originaleAndeler.stream()
            .noneMatch(a -> a.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold());
        var alleRevurderingAndelerHarReferanse = revurderingAndeler.stream()
            .allMatch(a -> a.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold());

        // Hvis alle 4 conditions er møtt kan andelene omfordeles selv om referanse ikke matcher. Dette er eneste unntak.
        return revurderingHarMaksEttSettAndeler && originalbehandlingHarMaksEttSettAndeler && ingenOriginaleAndelerHarReferanse
            && alleRevurderingAndelerHarReferanse;

    }

    private static boolean nøkkelHarMaksEtSettMedAndeler(List<BeregningsresultatAndel> andeler) {
        var andelerDerBrukerErMottaker = andeler.stream().filter(BeregningsresultatAndel::erBrukerMottaker).count();
        var andelerDerAGErMottaker = andeler.stream().filter(andel -> !andel.erBrukerMottaker()).count();
        return andelerDerBrukerErMottaker <= 1 && andelerDerAGErMottaker <= 1;
    }


}
