package no.nav.foreldrepenger.ytelse.beregning.regelmodell;

import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateInterval;

public record BeregningsresultatFeriepengerResultat(List<BeregningsresultatPeriode> beregningsresultatPerioder,
                                                    LocalDateInterval feriepengerPeriode) {
}
