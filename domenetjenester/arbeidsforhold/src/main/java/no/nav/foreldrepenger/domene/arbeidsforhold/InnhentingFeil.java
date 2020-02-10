package no.nav.foreldrepenger.domene.arbeidsforhold;

import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface InnhentingFeil extends DeklarerteFeil {

    InnhentingFeil FACTORY = FeilFactory.create(InnhentingFeil.class);

    @TekniskFeil(feilkode = "FP-112843", feilmelding = "Ignorerer Arena-sak uten %s, saksnummer: %s", logLevel = LogLevel.INFO)
    Feil ignorerArenaSakInfoLogg(String ignorert, Saksnummer saksnummer);

    @TekniskFeil(feilkode = "FP-597341", feilmelding = "Ignorerer Arena-sak med vedtakTom før vedtakFom, saksnummer: %s", logLevel = LogLevel.INFO)
    Feil ignorerArenaSakMedVedtakTomFørVedtakFom(Saksnummer saksnummer);

    @TekniskFeil(feilkode = "FP-464378", feilmelding = "Feil ved oppslag av aktørID for en arbeidgiver som er en privatperson registrert med fnr/dnr", logLevel = LogLevel.ERROR)
    Feil finnerIkkeAktørIdForArbeidsgiverSomErPrivatperson();
}

