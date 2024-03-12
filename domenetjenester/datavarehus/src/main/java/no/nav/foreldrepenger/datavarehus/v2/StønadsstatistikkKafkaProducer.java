package no.nav.foreldrepenger.datavarehus.v2;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaProperties;

@ApplicationScoped
public class StønadsstatistikkKafkaProducer {

    private static final Logger LOG = LoggerFactory.getLogger(StønadsstatistikkKafkaProducer.class);
    private static final Environment ENV = Environment.current();

    private Producer<String, String> producer;
    private String topicName;

    @Inject
    StønadsstatistikkKafkaProducer(@KonfigVerdi(value = "kafka.stonadsstatistikk.topic") String topicName) {
        this.topicName = topicName;
        this.producer = new KafkaProducer<>(KafkaProperties.forProducer());
    }

    StønadsstatistikkKafkaProducer() {
    }

    public void sendJson(String nøkkel, String json) {
        if (ENV.isLocal() || ENV.isDev()) {
            return;
        }
        LOG.info("Sender stønadsstatistikk vedtak med nøkkel {} på topic='{}'", nøkkel, topicName);
        runProducerWithSingleJson(new ProducerRecord<>(topicName, nøkkel, json));
    }

    private void runProducerWithSingleJson(ProducerRecord<String, String> record) {
        try {
            producer.send(record).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw kafkaPubliseringException(e);
        } catch (Exception e) {
            throw kafkaPubliseringException(e);
        }
    }

    private IntegrasjonException kafkaPubliseringException(Exception e) {
        return new IntegrasjonException("FP-HENDELSE-STØNAD", "Uventet feil ved sending til Kafka, topic " + topicName, e);
    }

}
