package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.vedtak.exception.TekniskException;

import java.util.List;

final class FastsettePerioderFeil {

    private FastsettePerioderFeil() {
    }

    static TekniskException manglendeOpprinneligPeriode(ForeldrepengerUttakPeriode periode) {
        return new TekniskException("FP-818121", "Fant ikke opprinnelig periode for ny periode " + periode);
    }

    static TekniskException manglendeOpprinneligAktivitet(ForeldrepengerUttakAktivitet nyAktivitet,
                                                   List<UttakResultatPeriodeAktivitetEntitet> aktiviteter) {
        var msg = String.format("Finner ingen aktivitet i opprinnelig for ny aktivitet %s %s", nyAktivitet,
            aktiviteter);
        return new TekniskException("FP-299466", msg);
    }
}
