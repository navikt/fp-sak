package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;

class TidslinjePeriodeWrapper {

    private BeregningsresultatPeriode revurderingPeriode;
    private BeregningsresultatPeriode originalPeriode;

    TidslinjePeriodeWrapper(BeregningsresultatPeriode revurderingPeriode, BeregningsresultatPeriode originalPeriode){
        this.revurderingPeriode = revurderingPeriode;
        this.originalPeriode = originalPeriode;
    }

    BeregningsresultatPeriode getRevurderingPeriode() {
        return revurderingPeriode;
    }

    BeregningsresultatPeriode getOriginalPeriode() {
        return originalPeriode;
    }

}
