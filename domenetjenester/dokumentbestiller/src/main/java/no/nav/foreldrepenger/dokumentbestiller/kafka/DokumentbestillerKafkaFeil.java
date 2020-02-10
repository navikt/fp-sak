package no.nav.foreldrepenger.dokumentbestiller.kafka;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface DokumentbestillerKafkaFeil extends DeklarerteFeil {

    DokumentbestillerKafkaFeil FACTORY = FeilFactory.create(DokumentbestillerKafkaFeil.class);

    @TekniskFeil(feilkode = "FP-533280", feilmelding = "Klarte ikke utlede ytelsetype: %s. Kan ikke bestille dokument", logLevel = LogLevel.ERROR)
    Feil fantIkkeYtelseType(String ytelseType);

}
