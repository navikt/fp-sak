package no.nav.foreldrepenger.mottak.publiserer.producer;

import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.RetriableException;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.IntegrasjonFeil;
import no.nav.vedtak.feil.deklarasjon.ManglerTilgangFeil;

public interface DialogHendelseKafkaProducerFeil extends DeklarerteFeil {

    DialogHendelseKafkaProducerFeil FACTORY = FeilFactory.create(DialogHendelseKafkaProducerFeil.class);

    @ManglerTilgangFeil(feilkode = "FP-HENDELSE-821006", feilmelding = "Feil i pålogging mot Kafka, topic:%s", logLevel = LogLevel.ERROR)
    Feil feilIPålogging(String topic, Exception e);

    @IntegrasjonFeil(feilkode = "FP-HENDELSE-925470", feilmelding = "Uventet feil ved sending til Kafka, topic:%s", logLevel = LogLevel.WARN)
    Feil uventetFeil(String topic, Exception e);

    @IntegrasjonFeil(feilkode = "FP-HENDELSE-127609", feilmelding = "Fikk transient feil mot Kafka, kan prøve igjen, topic:%s", logLevel = LogLevel.WARN)
    Feil retriableExceptionMotKaka(String topic, RetriableException e);

    @IntegrasjonFeil(feilkode = "FP-HENDELSE-811209", feilmelding = "Fikk feil mot Kafka, topic:%s", logLevel = LogLevel.WARN)
    Feil annenExceptionMotKafka(String topic, KafkaException e);
}
