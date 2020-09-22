package no.nav.foreldrepenger.mottak.vedtak.spokelse;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface SpokelseFeil extends DeklarerteFeil {

    SpokelseFeil FACTORY = FeilFactory.create(SpokelseFeil.class);

    @TekniskFeil(feilkode = "FP-180126", feilmelding = "SPokelse %s gir feil, ta opp med team sykepenger.", logLevel = LogLevel.ERROR)
    Feil feilfratjeneste(String tjeneste);

}
