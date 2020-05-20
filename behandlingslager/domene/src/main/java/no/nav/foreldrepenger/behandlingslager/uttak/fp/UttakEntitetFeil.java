package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import static no.nav.vedtak.feil.LogLevel.ERROR;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

interface UttakEntitetFeil extends DeklarerteFeil {
    UttakEntitetFeil FACTORY = FeilFactory.create(UttakEntitetFeil.class);

    @TekniskFeil(feilkode = "FP-661902", feilmelding = "Behandling må ha eksisterende uttaksresultat ved lagring av manuelt fastsatte perioder. Behandling id %s", logLevel = ERROR)
    Feil manueltFastettePerioderManglerEksisterendeResultat(Behandling behandling);

}
