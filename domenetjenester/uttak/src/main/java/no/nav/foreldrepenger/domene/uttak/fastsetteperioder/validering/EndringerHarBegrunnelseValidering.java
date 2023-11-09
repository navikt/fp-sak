package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;

class EndringerHarBegrunnelseValidering implements OverstyrUttakPerioderValidering {

    private final List<ForeldrepengerUttakPeriode> opprinnelig;

    EndringerHarBegrunnelseValidering(List<ForeldrepengerUttakPeriode> opprinnelig) {
        this.opprinnelig = opprinnelig;
    }

    @Override
    public void utfør(List<ForeldrepengerUttakPeriode> nyePerioder) {
        for (var periode : nyePerioder) {
            if (nullOrEmpty(periode.getBegrunnelse()) && harEndring(periode)) {
                throw OverstyrUttakValideringFeil.periodeManglerBegrunnelse();
            }
        }
    }

    private boolean nullOrEmpty(String begrunnelse) {
        return Objects.isNull(begrunnelse) || Objects.equals(begrunnelse, "");
    }

    private boolean harEndring(ForeldrepengerUttakPeriode periode) {
        for (var opprinneligPeriode : opprinnelig) {
            if (periode.erLikBortsettFraTrekkdager(opprinneligPeriode)) {
                return false;
            }
        }
        return true;
    }
}
