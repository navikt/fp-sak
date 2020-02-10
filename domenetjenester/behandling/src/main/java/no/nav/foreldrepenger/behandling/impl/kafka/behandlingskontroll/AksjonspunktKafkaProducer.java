package no.nav.foreldrepenger.behandling.impl.kafka.behandlingskontroll;

import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.log.mdc.MDCOperations;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class AksjonspunktKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(AksjonspunktKafkaProducer.class);
    private static final String CALLID_NAME = "Nav-CallId";

    Producer<String, String> producer;
    String topic;

    public AksjonspunktKafkaProducer() {
        // for CDI proxy
    }

    @Inject
    public AksjonspunktKafkaProducer(@KonfigVerdi("kafka.aksjonspunkthendelse.topic") String topic,
                                       @KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                       @KonfigVerdi("kafka.aksjonspunkthendelse.schema.registry.url") String schemaRegistryUrl,
                                       @KonfigVerdi("kafka.aksjonspunkthendelse.client.id") String clientId,
                                       @KonfigVerdi("systembruker.username") String username,
                                       @KonfigVerdi("systembruker.password") String password) {
        Properties properties = new Properties();

        properties.setProperty("bootstrap.servers", bootstrapServers);
        properties.setProperty("schema.registry.url", schemaRegistryUrl);
        properties.setProperty("client.id", clientId);

        setSecurity(username, properties);
        setUsernameAndPassword(username, password, properties);

        this.producer = createProducer(properties);
        this.topic = topic;

    }

    public void flush() {
        producer.flush();
    }

    void runProducerWithSingleJson(ProducerRecord<String, String> record) {
        try {
            producer.send(record)
                .get();
        } catch (InterruptedException e) {
            log.warn("Uventet feil ved sending til Kafka, topic:" + topic, e);
            Thread.currentThread().interrupt(); // reinterrupt
        } catch (ExecutionException e) {
            log.warn("Uventet feil ved sending til Kafka, topic:" + topic, e);
        } catch (AuthenticationException | AuthorizationException e) {
            log.warn("Feil i pålogging mot Kafka, topic:" + topic, e);
        } catch (RetriableException e) {
            log.warn("Fikk transient feil mot Kafka, kan prøve igjen, topic:" + topic, e);
        } catch (KafkaException e) {
            log.warn("Fikk feil mot Kafka, topic:" + topic, e);
        }
    }

    void setUsernameAndPassword(String username, String password, Properties properties) {
        if ((username != null && !username.isEmpty()) && (password != null && !password.isEmpty())) {
            String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            String jaasCfg = String.format(jaasTemplate, username, password);
            properties.setProperty("sasl.jaas.config", jaasCfg);
        }
    }

    Producer<String, String> createProducer(Properties properties) {
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }

    void setSecurity(String username, Properties properties) {
        if (username != null && !username.isEmpty()) {
            properties.setProperty("security.protocol", "SASL_SSL");
            properties.setProperty("sasl.mechanism", "PLAIN");
        }
    }

    public void sendJsonMedNøkkel(String nøkkel, String json) {
        String callId = MDCOperations.getCallId() != null ? MDCOperations.getCallId() : MDCOperations.generateCallId();
        runProducerWithSingleJson(new ProducerRecord<>(topic, null, nøkkel, json, new RecordHeaders().add(CALLID_NAME, callId.getBytes())));
    }
}
