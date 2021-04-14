package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

final class OmfordelDagsats {

    private OmfordelDagsats() {
        // skjul public constructor
    }

    static int beregnDagsatsArbeidsgiver(int revurderingArbeidsgiversDagsats, int revurderingBrukersDagsats, int originalBrukersDagsats) {
        var endringBrukersDagsats = revurderingBrukersDagsats - originalBrukersDagsats;
        if (KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(endringBrukersDagsats, revurderingArbeidsgiversDagsats)) {
            var bgAndelDagsats = revurderingBrukersDagsats + revurderingArbeidsgiversDagsats;
            return bgAndelDagsats - originalBrukersDagsats;
        }
        return revurderingArbeidsgiversDagsats;
    }

    static int beregnDagsatsBruker(int revurderingBrukersDagsats, int revurderingArbeidsgiversDagsats, int originalBrukersDagsats) {
        var endringDagsatsBruker = revurderingBrukersDagsats - originalBrukersDagsats;
        if (KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(endringDagsatsBruker, revurderingArbeidsgiversDagsats)) {
            return Math.max(revurderingBrukersDagsats, originalBrukersDagsats);
        }
        return revurderingBrukersDagsats;
    }


}
