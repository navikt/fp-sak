package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.kafka;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class SakOgBehandlingHendelseProducer {

    private static final Logger LOG = LoggerFactory.getLogger(SakOgBehandlingHendelseProducer.class);

    private String topic;
    private Producer<String, String> producer;

    SakOgBehandlingHendelseProducer() {
        // cdi
    }

    @Inject
    public SakOgBehandlingHendelseProducer(@KonfigVerdi("kafka.sakogbehandling.topic") String topicName,
                                           @KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                           @KonfigVerdi("schema.registry.url") String schemaRegistryUrl,
                                           @KonfigVerdi("systembruker.username") String username,
                                           @KonfigVerdi("systembruker.password") String password) {
        var properties = KafkaPropertiesUtil.opprettProperties(bootstrapServers, schemaRegistryUrl, getProducerClientId(topicName), username, password);
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
            throw new ManglerTilgangException("FP-HENDELSE-821007", "Feil i pålogging mot Kafka, topic: " + topic, e);
        } catch (RetriableException e) {
            throw new IntegrasjonException("FP-HENDELSE-127610", "Fikk transient feil mot Kafka, kan prøve igjen, topic " + topic, e);
        } catch (KafkaException e) {
            throw new IntegrasjonException("FP-HENDELSE-811210", "Fikk feil mot Kafka, topic: " + topic, e);
        }
    }

    private IntegrasjonException uventetException(Exception e) {
        return new IntegrasjonException("FP-HENDELSE-925471", "Uventet feil ved sending til Kafka, topic: " + topic, e);
    }

    private Producer<String, String> createProducer(Properties properties) {
        LOG.info("Oppretter producer for topic='{}', keyClass='{}', valueClass='{}'", topic, StringSerializer.class.getName(), StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }

    public void sendJsonMedNøkkel(String nøkkel, String json) {
        runProducerWithSingleJson(new ProducerRecord<>(topic, nøkkel, json));
    }

    private String getProducerClientId(String topicName) {
        return "KP-" + topicName;
    }

}
