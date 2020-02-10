package no.nav.foreldrepenger.domene.arbeidsforhold.person;

import static no.nav.vedtak.feil.LogLevel.ERROR;
import static no.nav.vedtak.feil.LogLevel.WARN;

import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.ManglerTilgangFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

interface TpsFeilmeldinger extends DeklarerteFeil {

    TpsFeilmeldinger FACTORY = FeilFactory.create(TpsFeilmeldinger.class);

    @TekniskFeil(feilkode = "FP-164687", feilmelding = "Person er ikke Bruker, kan ikke hente ut brukerinformasjon", logLevel = LogLevel.WARN)
    Feil ukjentBrukerType();

    @ManglerTilgangFeil(feilkode = "FP-432147", feilmelding = "TPS ikke tilgjengelig (sikkerhetsbegrensning)", logLevel = ERROR)
    Feil tpsUtilgjengeligSikkerhetsbegrensning(HentPersonSikkerhetsbegrensning cause);

    @TekniskFeil(feilkode = "FP-715017", feilmelding = "Fant ikke person i TPS", logLevel = WARN)
    Feil fantIkkePerson(HentPersonPersonIkkeFunnet cause);

    @TekniskFeil(feilkode = "FP-181237", feilmelding = "Fant ikke aktørId i TPS", logLevel = WARN)
    Feil fantIkkePersonForAktørId();
}
