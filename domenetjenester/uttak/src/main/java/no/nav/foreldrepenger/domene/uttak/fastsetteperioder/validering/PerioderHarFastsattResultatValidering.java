package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;

class PerioderHarFastsattResultatValidering implements OverstyrUttakPerioderValidering {

    @Override
    public void utfør(List<ForeldrepengerUttakPeriode> nyePerioder) {
        for (var periode : nyePerioder) {
            if (periode.getResultatType() == null || periode.getResultatType() == PeriodeResultatType.MANUELL_BEHANDLING) {
                throw OverstyrUttakValideringFeil.periodeManglerResultat();
            }
        }
    }
}
