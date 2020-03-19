package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import static no.nav.vedtak.feil.LogLevel.ERROR;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

interface FastsettePerioderFeil extends DeklarerteFeil {

    @TekniskFeil(feilkode = "FP-818121", feilmelding = "Fant ikke opprinnelig periode for ny periode %s", logLevel = ERROR)
    Feil manglendeOpprinneligPeriode(UttakResultatPeriode periode);

    @TekniskFeil(feilkode = "FP-299466", feilmelding = "Finner ingen aktivitet i opprinnelig for ny aktivitet %s %s", logLevel = ERROR)
    Feil manglendeOpprinneligAktivitet(UttakResultatPeriodeAktivitet nyAktivitet, List<UttakResultatPeriodeAktivitetEntitet> aktiviteter);
}
