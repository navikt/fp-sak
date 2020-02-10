package no.nav.foreldrepenger.historikk.kafka.feil;

import static no.nav.vedtak.feil.LogLevel.WARN;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface HistorikkFeil extends DeklarerteFeil {

    HistorikkFeil FACTORY = FeilFactory.create(HistorikkFeil.class);

    @TekniskFeil(feilkode = "FP-296632", feilmelding = "Klarte ikke deserialisere for klasse %s", logLevel = WARN)
    Feil klarteIkkeDeserialisere(String className, Throwable t);

    @TekniskFeil(feilkode = "FP-164754", feilmelding = "Mottok ugyldig journalpost ID %s", logLevel = WARN)
    Feil ugyldigJournalpost(String journalpost);
}
