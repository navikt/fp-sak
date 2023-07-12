package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.time.LocalDate;
import java.util.Collection;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.Beregningsresultat;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class HindreTilbaketrekkNårAlleredeUtbetalt {

    private HindreTilbaketrekkNårAlleredeUtbetalt() {
    }

    /**
     * Vi har utbetalt beregningsresultat (uten endringsdato eller feriepenger)
     *
     * @param beregningsgrunnlagTY {@link Beregningsresultat} basert på {@link no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag}et
     * @param tidslinje            {@link LocalDateTimeline} tidslinje for å sammeligne utbetalt og beregningsgrunnlag-versjonen av tilkjent ytelse
     * @param yrkesaktiviteter
     * @param skjæringstidspunkt
     * @return {@link Beregningsresultat}et vi ønsker å utbetale
     */
    public static BeregningsresultatEntitet reberegn(BeregningsresultatEntitet beregningsgrunnlagTY, LocalDateTimeline<BRAndelSammenligning> tidslinje, Collection<Yrkesaktivitet> yrkesaktiviteter, LocalDate skjæringstidspunkt) {
        // Map til regelmodell

        var utbetaltTY = BeregningsresultatEntitet.builder()
            .medRegelSporing(beregningsgrunnlagTY.getRegelSporing())
            .medRegelInput(beregningsgrunnlagTY.getRegelInput())
            .build();

        for (var segment : tidslinje.toSegments()) {
            HindreTilbaketrekkBeregningsresultatPeriode.omfordelPeriodeVedBehov(utbetaltTY, segment, yrkesaktiviteter, skjæringstidspunkt);
        }
        return utbetaltTY;
    }

}
