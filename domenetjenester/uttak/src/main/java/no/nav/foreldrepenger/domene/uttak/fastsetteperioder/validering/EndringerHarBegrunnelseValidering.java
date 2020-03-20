package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.vedtak.feil.FeilFactory;

class EndringerHarBegrunnelseValidering implements OverstyrUttakPerioderValidering {

    private List<ForeldrepengerUttakPeriode> opprinnelig;

    EndringerHarBegrunnelseValidering(List<ForeldrepengerUttakPeriode> opprinnelig) {
        this.opprinnelig = opprinnelig;
    }

    @Override
    public void utfør(List<ForeldrepengerUttakPeriode> nyePerioder) {
        for (ForeldrepengerUttakPeriode periode : nyePerioder) {
            if (nullOrEmpty(periode.getBegrunnelse()) && harEndring(periode)) {
                throw FeilFactory.create(OverstyrUttakValideringFeil.class).periodeManglerBegrunnelse().toException();
            }
        }
    }

    private boolean nullOrEmpty(String begrunnelse) {
        return Objects.isNull(begrunnelse) || Objects.equals(begrunnelse, "");
    }

    private boolean harEndring(ForeldrepengerUttakPeriode periode) {
        for (ForeldrepengerUttakPeriode opprinneligPeriode : opprinnelig) {
            if (periode.erLik(opprinneligPeriode)) {
                return false;
            }
        }
        return true;
    }
}
