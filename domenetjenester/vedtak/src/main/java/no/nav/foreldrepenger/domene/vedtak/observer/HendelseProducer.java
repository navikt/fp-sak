package no.nav.foreldrepenger.domene.vedtak.observer;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.exception.IntegrasjonException;

class HendelseProducer {

    private static final Logger LOG = LoggerFactory.getLogger(HendelseProducer.class);

    private final String topic;
    private final Producer<String, String> producer;

    HendelseProducer(String topicName,
                     String bootstrapServers,
                     String schemaRegistryUrl,
                     String username,
                     String password) {
        var properties = KafkaPropertiesUtil.opprettProperties(bootstrapServers, schemaRegistryUrl,
            getProducerClientId(topicName), username, password);

        this.topic = topicName;
        this.producer = createProducer(properties);
    }

    void sendJson(String json) {
        runProducerWithSingleJson(new ProducerRecord<>(topic, json));
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
        return new IntegrasjonException("FP-HENDELSE-925469", "Uventet feil ved sending til Kafka, topic " + topic, e);
    }

    private Producer<String, String> createProducer(Properties properties) {
        LOG.info("Oppretter producer for topic='{}', keyClass='{}', valueClass='{}'", topic, StringSerializer.class.getName(), StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }

    private String getProducerClientId(String topicName) {
        return "KP-" + topicName;
    }
}
