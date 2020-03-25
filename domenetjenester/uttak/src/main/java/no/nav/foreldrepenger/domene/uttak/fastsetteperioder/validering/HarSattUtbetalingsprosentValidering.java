package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.feil.FeilFactory;

class HarSattUtbetalingsprosentValidering implements OverstyrUttakPerioderValidering {

    private List<ForeldrepengerUttakPeriode> opprinnelig;

    HarSattUtbetalingsprosentValidering(List<ForeldrepengerUttakPeriode> opprinnelig) {
        this.opprinnelig = opprinnelig;
    }

    @Override
    public void utfør(List<ForeldrepengerUttakPeriode> nyePerioder) {
        for (ForeldrepengerUttakPeriode periode : nyePerioder) {
            if (manglerUtbetalingsprosent(periode) && opprinneligErManuell(periode)) {
                throw FeilFactory.create(OverstyrUttakValideringFeil.class).manglerUtbetalingsprosent(periode.getTidsperiode()).toException();
            }
        }
    }

    private boolean manglerUtbetalingsprosent(ForeldrepengerUttakPeriode periode) {
        return periode.getAktiviteter().stream().anyMatch(p -> p.getUtbetalingsprosent() == null);
    }

    private boolean opprinneligErManuell(ForeldrepengerUttakPeriode periode) {
        LocalDateInterval tidsperiode = periode.getTidsperiode();
        for (ForeldrepengerUttakPeriode opprinneligPeriode : opprinnelig) {
            if (opprinneligPeriode.getTidsperiode().overlaps(tidsperiode)) {
                return opprinneligPeriode.getResultatType().equals(PeriodeResultatType.MANUELL_BEHANDLING);
            }

        }
        return false;
    }

}
