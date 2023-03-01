package no.nav.foreldrepenger.ytelse.beregning.regelmodell;

public record FastsattBeregningsresultat(Beregningsresultat beregningsresultat,
                                         BeregningsresultatGrunnlag grunnlag,
                                         String regelInput,
                                         String regelSporing,
                                         String versjon) {

}



