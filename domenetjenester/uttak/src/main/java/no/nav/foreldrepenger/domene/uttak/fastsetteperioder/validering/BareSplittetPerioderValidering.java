package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.vedtak.feil.FeilFactory;

class BareSplittetPerioderValidering implements OverstyrUttakPerioderValidering {

    private List<ForeldrepengerUttakPeriode> opprinnelig;

    BareSplittetPerioderValidering(List<ForeldrepengerUttakPeriode> opprinnelig) {
        this.opprinnelig = opprinnelig;
    }

    @Override
    public void utfør(List<ForeldrepengerUttakPeriode> nyePerioder) {
        for (ForeldrepengerUttakPeriode nyPeriode : nyePerioder) {
            validerAtAlleDagerINyErIOpprinnelig(nyPeriode);
        }

        for (ForeldrepengerUttakPeriode opprinneligPeriode : opprinnelig) {
            validerAtAlleDagerIOpprinneligErINy(opprinneligPeriode, nyePerioder);
        }
    }

    private void validerAtAlleDagerIOpprinneligErINy(ForeldrepengerUttakPeriode opprinnelig, List<ForeldrepengerUttakPeriode> nyePerioder) {
        validerAtAlleDagerIPeriodeFinnesIPerioder(opprinnelig, nyePerioder);
    }

    private void validerAtAlleDagerINyErIOpprinnelig(ForeldrepengerUttakPeriode nyPeriode) {
        validerAtAlleDagerIPeriodeFinnesIPerioder(nyPeriode, opprinnelig);
    }

    private void validerAtAlleDagerIPeriodeFinnesIPerioder(ForeldrepengerUttakPeriode periode, List<ForeldrepengerUttakPeriode> perioder) {
        LocalDate dato = periode.getTidsperiode().getFomDato();
        while (dato.isBefore(periode.getTidsperiode().getTomDato()) || dato.isEqual(periode.getTidsperiode().getTomDato())) {
            if (!datoFinnesBareEnGangIPerioder(dato, perioder)) {
                throwException();
            }
            dato = dato.plusDays(1);
        }
    }

    private boolean datoFinnesBareEnGangIPerioder(LocalDate dato, List<ForeldrepengerUttakPeriode> perioder) {
        int antallFunnet = 0;
        for (ForeldrepengerUttakPeriode periode : perioder) {
            if (periode.getTidsperiode().encloses(dato)) {
                antallFunnet++;
            }
        }
        return antallFunnet == 1;
    }

    private void throwException() {
        throw FeilFactory.create(OverstyrUttakValideringFeil.class).ugyldigSplittingAvPeriode().toException();
    }
}
