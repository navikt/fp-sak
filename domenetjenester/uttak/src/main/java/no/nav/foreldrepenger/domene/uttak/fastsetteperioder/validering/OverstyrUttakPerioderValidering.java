package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;

import java.util.List;

interface OverstyrUttakPerioderValidering {

    void utfør(List<ForeldrepengerUttakPeriode> nyePerioder);
}
