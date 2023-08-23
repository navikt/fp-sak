package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;

import java.util.List;

class HarSattUtbetalingsgradValidering implements OverstyrUttakPerioderValidering {

    private final List<ForeldrepengerUttakPeriode> opprinnelig;

    HarSattUtbetalingsgradValidering(List<ForeldrepengerUttakPeriode> opprinnelig) {
        this.opprinnelig = opprinnelig;
    }

    @Override
    public void utfør(List<ForeldrepengerUttakPeriode> nyePerioder) {
        for (var periode : nyePerioder) {
            if (manglerUtbetalingsgrad(periode) && opprinneligErManuell(periode)) {
                throw OverstyrUttakValideringFeil.manglerUtbetalingsgrad(periode.getTidsperiode());
            }
        }
    }

    private boolean manglerUtbetalingsgrad(ForeldrepengerUttakPeriode periode) {
        return periode.getAktiviteter().stream().anyMatch(p -> p.getUtbetalingsgrad() == null);
    }

    private boolean opprinneligErManuell(ForeldrepengerUttakPeriode periode) {
        var tidsperiode = periode.getTidsperiode();
        for (var opprinneligPeriode : opprinnelig) {
            if (opprinneligPeriode.getTidsperiode().overlaps(tidsperiode)) {
                return opprinneligPeriode.getResultatType().equals(PeriodeResultatType.MANUELL_BEHANDLING);
            }

        }
        return false;
    }

}
