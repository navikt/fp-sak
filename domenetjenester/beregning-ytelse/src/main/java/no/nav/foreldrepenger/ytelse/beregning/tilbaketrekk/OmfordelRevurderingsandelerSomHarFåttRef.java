package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class OmfordelRevurderingsandelerSomHarFåttRef {

    private OmfordelRevurderingsandelerSomHarFåttRef() {
        // SKjuler default
    }

    public static List<EndringIBeregningsresultat> omfordel(BRNøkkelMedAndeler revurderingNøkkelMedAndeler, BRNøkkelMedAndeler originalNøkkelMedAndeler) {
        List<EndringIBeregningsresultat> list = new ArrayList<>();

        var revurderingBrukersAndel = revurderingNøkkelMedAndeler.getBrukersAndelerTilknyttetNøkkel().stream().findFirst();
        var revurderingArbeidgiversAndel = revurderingNøkkelMedAndeler.getArbeidsgiversAndelerTilknyttetNøkkel().stream().findFirst();

        var originalBrukersAndel = originalNøkkelMedAndeler.getBrukersAndelerTilknyttetNøkkel().stream().findFirst();

        int originalBrukersDagsats = originalBrukersAndel.map(BeregningsresultatAndel::getDagsats).orElse(0);

        revurderingBrukersAndel.flatMap(andel -> omfordelBrukersAndel(andel, revurderingArbeidgiversAndel, originalBrukersDagsats)).ifPresent(list::add);
        revurderingArbeidgiversAndel.flatMap(andel -> omfordelArbeidsgiversAndel(revurderingBrukersAndel, andel, originalBrukersDagsats)).ifPresent(list::add);

        return list;
    }

    private static Optional<EndringIBeregningsresultat> omfordelBrukersAndel(BeregningsresultatAndel brukersAndel, Optional<BeregningsresultatAndel> arbeidsgiversAndel, int originalBrukersDagsats) {
        var revurderingBrukersDagsats = brukersAndel.getDagsats();
        int revurderingArbeidsgiverDagsats = arbeidsgiversAndel.map(BeregningsresultatAndel::getDagsats).orElse(0);
        var reberegnetBrukersDagsats = OmfordelDagsats.beregnDagsatsBruker(revurderingBrukersDagsats, revurderingArbeidsgiverDagsats, originalBrukersDagsats);
        if (erDagsatsEndret(brukersAndel, reberegnetBrukersDagsats)) {
            var endring = new EndringIBeregningsresultat(brukersAndel, reberegnetBrukersDagsats);
            return Optional.of(endring);
        }
        return Optional.empty();
    }

    private static Optional<EndringIBeregningsresultat> omfordelArbeidsgiversAndel(Optional<BeregningsresultatAndel> brukersAndel, BeregningsresultatAndel revurderingArbeidgiversAndel, int originalBrukersDagsats) {
        int revurderingBrukersDagsats = brukersAndel.map(BeregningsresultatAndel::getDagsats).orElse(0);
        var revurderingArbeidsgiverDagsats = revurderingArbeidgiversAndel.getDagsats();

        var reberegnetArbeidsgiversDagsats = OmfordelDagsats.beregnDagsatsArbeidsgiver(revurderingArbeidsgiverDagsats, revurderingBrukersDagsats, originalBrukersDagsats);
        if (erDagsatsEndret(revurderingArbeidgiversAndel, reberegnetArbeidsgiversDagsats)) {
            var endring = new EndringIBeregningsresultat(revurderingArbeidgiversAndel, reberegnetArbeidsgiversDagsats);
            return Optional.of(endring);
        }
        return Optional.empty();
    }

    private static boolean erDagsatsEndret(BeregningsresultatAndel andel, int omberegnetDagsats) {
        return omberegnetDagsats != andel.getDagsats();
    }

}
