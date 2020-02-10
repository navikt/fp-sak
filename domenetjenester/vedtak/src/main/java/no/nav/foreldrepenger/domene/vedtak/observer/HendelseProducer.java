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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.topic.Topic;

public class HendelseProducer {

    private static final Logger log = LoggerFactory.getLogger(HendelseProducer.class);

    private final Topic topic;
    private final Producer<String, String> producer;

    public HendelseProducer(Topic topic,
                            String bootstrapServers,
                            String schemaRegistryUrl,
                            String username,
                            String password) {
        Properties properties = KafkaPropertiesUtil.opprettProperties(bootstrapServers, schemaRegistryUrl, topic.getProducerClientId(), username, password);

        this.topic = topic;
        this.producer = createProducer(properties);
    }

    public void flushAndClose() {
        producer.flush();
        producer.close();
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
            throw HendelseKafkaProducerFeil.FACTORY.uventetFeil(topic.getTopic(), e).toException();
        } catch (ExecutionException e) {
            throw HendelseKafkaProducerFeil.FACTORY.uventetFeil(topic.getTopic(), e).toException();
        } catch (AuthenticationException | AuthorizationException e) {
            throw HendelseKafkaProducerFeil.FACTORY.feilIPålogging(topic.getTopic(), e).toException();
        } catch (RetriableException e) {
            throw HendelseKafkaProducerFeil.FACTORY.retriableExceptionMotKaka(topic.getTopic(), e).toException();
        } catch (KafkaException e) {
            throw HendelseKafkaProducerFeil.FACTORY.annenExceptionMotKafka(topic.getTopic(), e).toException();
        }
    }

    private Producer<String, String> createProducer(Properties properties) {
        log.info("Oppretter producer for topic='{}', keyClass='{}', valueClass='{}'", topic.getTopic(), topic.getSerdeKey(), topic.getSerdeValue());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, topic.getSerdeKey().serializer().getClass().getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, topic.getSerdeValue().serializer().getClass().getName());
        return new KafkaProducer<>(properties);
    }

    public void sendJson(String json) {
        runProducerWithSingleJson(new ProducerRecord<>(topic.getTopic(), json));
    }

    public void sendJsonMedNøkkel(String nøkkel, String json) {
        runProducerWithSingleJson(new ProducerRecord<>(topic.getTopic(), nøkkel, json));
    }

}
