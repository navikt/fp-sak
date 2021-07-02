package no.nav.foreldrepenger.domene.risikoklassifisering.kafka.config;

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

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.ManglerTilgangException;

@ApplicationScoped
class RisikoklassifiseringMeldingProducer  {

    private Producer<String, String> producer;
    private String topic;

    public RisikoklassifiseringMeldingProducer() {
        // for CDI proxy
    }

    @Inject
    public RisikoklassifiseringMeldingProducer(@KonfigVerdi("kafka.risikoklassifisering.topic") String topicName,
                                               @KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                               @KonfigVerdi("schema.registry.url") String schemaRegistryUrl,
                                               @KonfigVerdi("systembruker.username") String username,
                                               @KonfigVerdi("systembruker.password") String password) {
        var properties = new Properties();

        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("schema.registry.url", schemaRegistryUrl);
        properties.put("client.id", getProducerClientId(topicName));

        setSecurity(username, properties);
        setUsernameAndPassword(username, password, properties);

        this.producer = createProducer(properties);
        this.topic = topicName;
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
            producer.send(record)
                .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IntegrasjonException("FP-RISK-925469", "Uventet feil ved sending til Kafka, topic: " + topic, e);
        } catch (AuthenticationException | AuthorizationException e) {
            throw new ManglerTilgangException("FP-RISK-821005", "Feil i pålogging mot Kafka, topic: " + topic, e);
        } catch (RetriableException e) {
            throw new IntegrasjonException("FP-RISK-127608", "Fikk transient feil mot Kafka, kan prøve igjen, topic: " + topic, e);
        } catch (KafkaException e) {
            throw new IntegrasjonException("FP-RISK-811208", "Fikk feil mot Kafka, topic: " + topic, e);
        }
    }

    private void setUsernameAndPassword(String username, String password, Properties properties) {
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            var jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            var jaasCfg = String.format(jaasTemplate, username, password);
            properties.put("sasl.jaas.config", jaasCfg);
        }
    }

    private Producer<String, String> createProducer(Properties properties) {
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }

    private void setSecurity(String username, Properties properties) {
        if (username != null && !username.isEmpty()) {
            properties.put("security.protocol", "SASL_SSL");
            properties.put("sasl.mechanism", "PLAIN");
        }
    }

    public void sendJson(String json) {
        runProducerWithSingleJson(new ProducerRecord<>(topic, json));
    }

    public void sendJsonMedNøkkel(String nøkkel, String json) {
        runProducerWithSingleJson(new ProducerRecord<>(topic, nøkkel, json));
    }

    private final String getProducerClientId(String topicName) {
        return "KP-" + topicName;
    }
}
