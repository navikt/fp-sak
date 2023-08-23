package no.nav.foreldrepenger.domene.vedtak.observer;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaProperties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class VedtakHendelseKafkaProducer {

    private static final Logger LOG = LoggerFactory.getLogger(VedtakHendelseKafkaProducer.class);

    private Producer<String, String> producer;
    private String topicName;

    public VedtakHendelseKafkaProducer() {
    }

    @Inject
    public VedtakHendelseKafkaProducer(@KonfigVerdi("kafka.fattevedtak.topic") String topicName) {
        this.topicName = topicName;
        this.producer = new KafkaProducer<>(KafkaProperties.forProducer());
    }

    void sendJson(String nøkkel, String json) {
        LOG.info("Sender vedtak med nøkkel {} på topic='{}'", nøkkel, topicName);
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
        return new IntegrasjonException("FP-HENDELSE-925469", "Uventet feil ved sending til Kafka, topic " + topicName, e);
    }

}
