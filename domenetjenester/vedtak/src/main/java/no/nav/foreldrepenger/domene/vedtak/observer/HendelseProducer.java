package no.nav.foreldrepenger.domene.vedtak.observer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.ManglerTilgangException;

public class HendelseProducer {

    private static final Logger LOG = LoggerFactory.getLogger(HendelseProducer.class);

    private final String topic;
    private final Producer<String, String> producer;

    public HendelseProducer(String topicName,
                            String bootstrapServers,
                            String schemaRegistryUrl,
                            String username,
                            String password) {
        var properties = KafkaPropertiesUtil.opprettProperties(bootstrapServers, schemaRegistryUrl,
            getProducerClientId(topicName), username, password);

        this.topic = topicName;
        this.producer = createProducer(properties);
    }

    public void flush() {
        producer.flush();
    }

    private void runProducerWithSingleJson(ProducerRecord<String, String> record) {
        try {
            @SuppressWarnings("unused")
            var recordMetadata = producer.send(record).get(); //NOSONAR
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw uventetException(e);
        } catch (ExecutionException e) {
            throw uventetException(e);
        } catch (AuthenticationException | AuthorizationException e) {
            throw new ManglerTilgangException("FP-HENDELSE-821005", "Feil i pålogging mot Kafka, topic:" + topic, e);
        } catch (RetriableException e) {
            throw new IntegrasjonException("FP-HENDELSE-127608", "Fikk transient feil mot Kafka, kan prøve igjen,"
                + " topic: " + topic, e);
        } catch (KafkaException e) {
            throw new IntegrasjonException("FP-HENDELSE-811208", "Fikk feil mot Kafka, topic: " + topic, e);
        }
    }

    private IntegrasjonException uventetException(Exception e) {
        return new IntegrasjonException("FP-HENDELSE-925469", "Uventet feil ved sending til Kafka, topic " + topic, e);
    }

    private Producer<String, String> createProducer(Properties properties) {
        LOG.info("Oppretter producer for topic='{}', keyClass='{}', valueClass='{}'", topic, StringSerializer.class.getName(), StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }

    public void sendJson(String json) {
        runProducerWithSingleJson(new ProducerRecord<>(topic, json));
    }

    public String getProducerClientId(String topicName) {
        return "KP-" + topicName;
    }
}
