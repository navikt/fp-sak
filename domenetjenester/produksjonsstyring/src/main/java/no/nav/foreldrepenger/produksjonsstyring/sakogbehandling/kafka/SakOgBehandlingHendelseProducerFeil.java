package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.kafka;

import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.RetriableException;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.IntegrasjonFeil;
import no.nav.vedtak.feil.deklarasjon.ManglerTilgangFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface SakOgBehandlingHendelseProducerFeil extends DeklarerteFeil {

    SakOgBehandlingHendelseProducerFeil FACTORY = FeilFactory.create(SakOgBehandlingHendelseProducerFeil.class);

    @ManglerTilgangFeil(feilkode = "FP-HENDELSE-821007", feilmelding = "Feil i pålogging mot Kafka, topic:%s", logLevel = LogLevel.ERROR)
    Feil feilIPålogging(String topic, Exception e);

    @IntegrasjonFeil(feilkode = "FP-HENDELSE-925471", feilmelding = "Uventet feil ved sending til Kafka, topic:%s", logLevel = LogLevel.WARN)
    Feil uventetFeil(String topic, Exception e);

    @IntegrasjonFeil(feilkode = "FP-HENDELSE-127610", feilmelding = "Fikk transient feil mot Kafka, kan prøve igjen, topic:%s", logLevel = LogLevel.WARN)
    Feil retriableExceptionMotKaka(String topic, RetriableException e);

    @IntegrasjonFeil(feilkode = "FP-HENDELSE-811210", feilmelding = "Fikk feil mot Kafka, topic:%s", logLevel = LogLevel.WARN)
    Feil annenExceptionMotKafka(String topic, KafkaException e);

    @TekniskFeil(feilkode = "FP-190497", feilmelding = "Kunne ikke serialisere til json.", logLevel = LogLevel.WARN)
    Feil kanIkkeSerialisere(JsonProcessingException e);
}
