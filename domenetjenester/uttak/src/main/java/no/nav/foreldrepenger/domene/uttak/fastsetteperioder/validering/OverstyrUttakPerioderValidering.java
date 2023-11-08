package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import java.util.List;

import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;

interface OverstyrUttakPerioderValidering {

    void utfør(List<ForeldrepengerUttakPeriode> nyePerioder);
}
