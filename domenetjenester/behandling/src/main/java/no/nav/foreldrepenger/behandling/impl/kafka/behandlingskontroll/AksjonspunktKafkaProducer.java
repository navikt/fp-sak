package no.nav.foreldrepenger.behandling.impl.kafka.behandlingskontroll;

import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class AksjonspunktKafkaProducer {

    private static final Logger LOG = LoggerFactory.getLogger(AksjonspunktKafkaProducer.class);
    private static final String CALLID_NAME = "Nav-CallId";

    Producer<String, String> producer;
    String topic;

    public AksjonspunktKafkaProducer() {
        // for CDI proxy
    }

    @Inject
    public AksjonspunktKafkaProducer(@KonfigVerdi("kafka.aksjonspunkthendelse.topic") String topicName,
            @KonfigVerdi("bootstrap.servers") String bootstrapServers,
            @KonfigVerdi("schema.registry.url") String schemaRegistryUrl,
            @KonfigVerdi("systembruker.username") String username,
            @KonfigVerdi("systembruker.password") String password) {
        var properties = new Properties();

        properties.setProperty("bootstrap.servers", bootstrapServers);
        properties.setProperty("schema.registry.url", schemaRegistryUrl);
        properties.setProperty("client.id", getProducerClientId(topicName));

        setSecurity(username, properties);
        setUsernameAndPassword(username, password, properties);

        this.producer = createProducer(properties);
        this.topic = topicName;

    }

    public void flush() {
        producer.flush();
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
        return new IntegrasjonException("FP-HENDELSE-925473", "Uventet feil ved sending til Kafka, topic " + topic, e);
    }

    void setUsernameAndPassword(String username, String password, Properties properties) {
        if (((username != null) && !username.isEmpty()) && ((password != null) && !password.isEmpty())) {
            var jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            var jaasCfg = String.format(jaasTemplate, username, password);
            properties.setProperty("sasl.jaas.config", jaasCfg);
        }
    }

    Producer<String, String> createProducer(Properties properties) {
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }

    void setSecurity(String username, Properties properties) {
        if ((username != null) && !username.isEmpty()) {
            properties.setProperty("security.protocol", "SASL_SSL");
            properties.setProperty("sasl.mechanism", "PLAIN");
        }
    }

    public void sendJsonMedNøkkel(String nøkkel, String json) {
        var callId = MDCOperations.getCallId() != null ? MDCOperations.getCallId() : MDCOperations.generateCallId();
        runProducerWithSingleJson(new ProducerRecord<>(topic, null, nøkkel, json, new RecordHeaders().add(CALLID_NAME, callId.getBytes())));
    }

    private String getProducerClientId(String topicName) {
        return "KP-" + topicName;
    }
}
