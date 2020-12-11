package no.nav.foreldrepenger.ytelse.beregning.endringsdato;

import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatPeriodeEndringModell;

class TidslinjePeriodeWrapper {

    private final BeregningsresultatPeriodeEndringModell revurderingPeriode;
    private final BeregningsresultatPeriodeEndringModell originalPeriode;

    TidslinjePeriodeWrapper(BeregningsresultatPeriodeEndringModell revurderingPeriode,
                            BeregningsresultatPeriodeEndringModell originalPeriode){
        this.revurderingPeriode = revurderingPeriode;
        this.originalPeriode = originalPeriode;
    }

    BeregningsresultatPeriodeEndringModell getRevurderingPeriode() {
        return revurderingPeriode;
    }

    BeregningsresultatPeriodeEndringModell getOriginalPeriode() {
        return originalPeriode;
    }

}
