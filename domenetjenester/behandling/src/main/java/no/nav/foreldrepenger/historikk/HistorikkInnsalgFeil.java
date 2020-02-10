package no.nav.foreldrepenger.historikk;

import static no.nav.vedtak.feil.LogLevel.ERROR;

import java.util.List;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

interface HistorikkInnsalgFeil extends DeklarerteFeil {
    HistorikkInnsalgFeil FACTORY = FeilFactory.create(HistorikkInnsalgFeil.class);

    @TekniskFeil(feilkode = "FP-876694", feilmelding = "For type %s, mangler felter %s for historikkinnslag.", logLevel = ERROR)
    Feil manglerFeltForHistorikkInnslag(String type, List<String> manglendeFelt);

    @TekniskFeil(feilkode = "FP-876693", feilmelding = "For type %s, forventer minst et felt av type %s", logLevel = ERROR)
    Feil manglerMinstEtFeltForHistorikkinnslag(String type, List<String> manglendeFelt);

    @TekniskFeil(feilkode = "FP-876692", feilmelding = "Ukjent historikkinnslagstype: %s", logLevel = ERROR)
    Feil ukjentHistorikkinnslagType(String kode);
}
