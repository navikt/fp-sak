package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.vedtak.feil.FeilFactory;

class PerioderHarFastsattResultatValidering implements OverstyrUttakPerioderValidering {

    @Override
    public void utfør(List<ForeldrepengerUttakPeriode> nyePerioder) {
        for (ForeldrepengerUttakPeriode periode : nyePerioder) {
            if (periode.getResultatType() == null || periode.getResultatType() == PeriodeResultatType.IKKE_FASTSATT) {
                throw FeilFactory.create(OverstyrUttakValideringFeil.class).periodeManglerResultat().toException();
            }
        }
    }
}
