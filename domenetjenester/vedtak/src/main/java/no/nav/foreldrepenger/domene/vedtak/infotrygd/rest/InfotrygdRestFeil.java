package no.nav.foreldrepenger.domene.vedtak.infotrygd.rest;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface InfotrygdRestFeil extends DeklarerteFeil {

    InfotrygdRestFeil FACTORY = FeilFactory.create(InfotrygdRestFeil.class);

    @TekniskFeil(feilkode = "FP-180125", feilmelding = "Tjeneste %s gir feil", logLevel = LogLevel.ERROR)
    Feil feilfratjeneste(String tjeneste);

}
