package no.nav.foreldrepenger.domene.person.tps;

import static no.nav.vedtak.feil.LogLevel.ERROR;
import static no.nav.vedtak.feil.LogLevel.WARN;

import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningSikkerhetsbegrensing;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.ManglerTilgangFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface TpsFeilmeldinger extends DeklarerteFeil {

    TpsFeilmeldinger FACTORY = FeilFactory.create(TpsFeilmeldinger.class);

    @TekniskFeil(feilkode = "FP-181235", feilmelding = "Fant ikke aktørId i TPS", logLevel = WARN)
    Feil fantIkkePersonForAktørId();

    @ManglerTilgangFeil(feilkode = "FP-115180", feilmelding = "TPS ikke tilgjengelig (sikkerhetsbegrensning)", logLevel = ERROR)
    Feil tpsUtilgjengeligGeografiskTilknytningSikkerhetsbegrensing(HentGeografiskTilknytningSikkerhetsbegrensing cause);

    @TekniskFeil(feilkode = "FP-349049", feilmelding = "Fant ikke geografisk informasjon for person", logLevel = WARN)
    Feil geografiskTilknytningIkkeFunnet(HentGeografiskTilknytningPersonIkkeFunnet cause);
}
