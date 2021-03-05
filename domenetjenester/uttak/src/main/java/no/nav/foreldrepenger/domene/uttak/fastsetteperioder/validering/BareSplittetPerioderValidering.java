package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;

class BareSplittetPerioderValidering implements OverstyrUttakPerioderValidering {

    private final List<ForeldrepengerUttakPeriode> opprinnelig;

    BareSplittetPerioderValidering(List<ForeldrepengerUttakPeriode> opprinnelig) {
        this.opprinnelig = opprinnelig;
    }

    @Override
    public void utfør(List<ForeldrepengerUttakPeriode> nyePerioder) {
        for (var nyPeriode : nyePerioder) {
            validerAtAlleDagerINyErIOpprinnelig(nyPeriode);
        }

        for (var opprinneligPeriode : opprinnelig) {
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
        var dato = periode.getTidsperiode().getFomDato();
        while (dato.isBefore(periode.getTidsperiode().getTomDato()) || dato.isEqual(periode.getTidsperiode().getTomDato())) {
            if (!datoFinnesBareEnGangIPerioder(dato, perioder)) {
                throw OverstyrUttakValideringFeil.ugyldigSplittingAvPeriode();
            }
            dato = dato.plusDays(1);
        }
    }

    private boolean datoFinnesBareEnGangIPerioder(LocalDate dato, List<ForeldrepengerUttakPeriode> perioder) {
        var antallFunnet = 0;
        for (var periode : perioder) {
            if (periode.getTidsperiode().encloses(dato)) {
                antallFunnet++;
            }
        }
        return antallFunnet == 1;
    }

}
