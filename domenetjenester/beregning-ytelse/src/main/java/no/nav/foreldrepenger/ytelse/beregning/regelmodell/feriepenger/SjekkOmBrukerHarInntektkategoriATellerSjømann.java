package no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

class SjekkOmBrukerHarInntektkategoriATellerSjømann extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    public static final String ID = "FP_BR 8.1";
    public static final String BESKRIVELSE = "Er bruker beregnet som arbeidstaker ved skjæringstidspunktet?";


    SjekkOmBrukerHarInntektkategoriATellerSjømann() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        var arbeidstakerVedSTP = regelModell.erArbeidstakerVedSkjæringstidspunkt();
        var utbetalingForArbeidstakerAndel = regelModell.getInntektskategorier().stream()
            .anyMatch(Inntektskategori::erArbeidstakerEllerSjømann);
        return arbeidstakerVedSTP && utbetalingForArbeidstakerAndel ? ja() : nei();
    }
}
