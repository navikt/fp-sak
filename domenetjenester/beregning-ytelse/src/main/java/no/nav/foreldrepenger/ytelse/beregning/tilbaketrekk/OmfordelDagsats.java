package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

final class OmfordelDagsats {

    private OmfordelDagsats() {
        // skjul public constructor
    }

    static int beregnDagsatsArbeidsgiver(int revurderingArbeidsgiversDagsats, int revurderingBrukersDagsats, int originalBrukersDagsats) {
        int endringBrukersDagsats = revurderingBrukersDagsats - originalBrukersDagsats;
        if (KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(endringBrukersDagsats, revurderingArbeidsgiversDagsats)) {
            int bgAndelDagsats = revurderingBrukersDagsats + revurderingArbeidsgiversDagsats;
            return bgAndelDagsats - originalBrukersDagsats;
        } else {
            return revurderingArbeidsgiversDagsats;
        }
    }

    static int beregnDagsatsBruker(int revurderingBrukersDagsats, int revurderingArbeidsgiversDagsats, int originalBrukersDagsats) {
        int endringDagsatsBruker = revurderingBrukersDagsats - originalBrukersDagsats;
        if (KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(endringDagsatsBruker, revurderingArbeidsgiversDagsats)) {
            return Math.max(revurderingBrukersDagsats, originalBrukersDagsats);
        } else {
            return revurderingBrukersDagsats;
        }
    }


}
