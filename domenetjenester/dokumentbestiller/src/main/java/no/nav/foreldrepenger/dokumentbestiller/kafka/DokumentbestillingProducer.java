package no.nav.foreldrepenger.dokumentbestiller.kafka;

import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.IntegrasjonException;

@ApplicationScoped
class DokumentbestillingProducer {

    private Producer<String, String> producer;
    private String topic;

    DokumentbestillingProducer() {
        // for CDI proxy
    }

    @Inject
    public DokumentbestillingProducer(@KonfigVerdi("kafka.dokumentbestilling.topic") String topic,
                                      @KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                      @KonfigVerdi("schema.registry.url") String schemaRegistryUrl,
                                      @KonfigVerdi("systembruker.username") String username,
                                      @KonfigVerdi("systembruker.password") String password) {
        var properties = new Properties();

        var clientId = "KP-" + topic;
        properties.setProperty("bootstrap.servers", bootstrapServers);
        properties.setProperty("schema.registry.url", schemaRegistryUrl);
        properties.setProperty("client.id", clientId);

        setSecurity(username, properties);
        setUsernameAndPassword(username, password, properties);

        this.producer = createProducer(properties);
        this.topic = topic;

    }

    void publiserDokumentbestillingJson(String json) {
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
        return new IntegrasjonException("FP-HENDELSE-925476", "Uventet feil ved sending til Kafka, topic " + topic, e);
    }

    private void setUsernameAndPassword(String username, String password, Properties properties) {
        if ((username != null && !username.isEmpty())
                && (password != null && !password.isEmpty())) {
            var jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            var jaasCfg = String.format(jaasTemplate, username, password);
            properties.setProperty("sasl.jaas.config", jaasCfg);
        }
    }

    private Producer<String, String> createProducer(Properties properties) {
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }

    private void setSecurity(String username, Properties properties) {
        if (username != null && !username.isEmpty()) {
            properties.setProperty("security.protocol", "SASL_SSL");
            properties.setProperty("sasl.mechanism", "PLAIN");
        }
    }
}
