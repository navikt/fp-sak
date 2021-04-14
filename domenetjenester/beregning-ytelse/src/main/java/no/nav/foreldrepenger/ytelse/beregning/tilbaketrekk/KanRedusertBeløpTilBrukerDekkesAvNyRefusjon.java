package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

class KanRedusertBeløpTilBrukerDekkesAvNyRefusjon {

    private KanRedusertBeløpTilBrukerDekkesAvNyRefusjon() {
        // skjul public constructor
    }

    static boolean vurder(int endringIDagsatsBruker, int revurderingRefusjon) {
        var erEndringIDagsatsbruker = endringIDagsatsBruker < 0;
        var erEndringForBrukerMindreEnnNyRefusjon = Math.abs(endringIDagsatsBruker) <= revurderingRefusjon;
        var finnesNyRefusjon = revurderingRefusjon > 0;

        return erEndringIDagsatsbruker && erEndringForBrukerMindreEnnNyRefusjon && finnesNyRefusjon;
    }
}
