package no.nav.foreldrepenger.ytelse.beregning.regelmodell;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakResultat;
import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;

@RuleDocumentationGrunnlag
public record BeregningsresultatGrunnlag(Beregningsgrunnlag beregningsgrunnlag, UttakResultat uttakResultat) {
}
