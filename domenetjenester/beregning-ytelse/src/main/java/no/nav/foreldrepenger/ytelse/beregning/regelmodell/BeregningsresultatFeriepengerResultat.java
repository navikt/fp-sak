package no.nav.foreldrepenger.ytelse.beregning.regelmodell;

import no.nav.fpsak.tidsserie.LocalDateInterval;

import java.util.List;

public record BeregningsresultatFeriepengerResultat(List<BeregningsresultatPeriode> beregningsresultatPerioder,
                                                    LocalDateInterval feriepengerPeriode) {
}
