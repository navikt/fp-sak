package no.nav.foreldrepenger.mottak.publiserer.producer;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.IntegrasjonException;

import no.nav.vedtak.log.mdc.MDCOperations;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.Properties;


@ApplicationScoped
public class DialogHendelseProducer {

    private static final Logger LOG = LoggerFactory.getLogger(DialogHendelseProducer.class);
    private static final Environment ENV = Environment.current();
    private static final boolean IS_DEPLOYMENT = ENV.isProd() || ENV.isDev();

    private Producer<String, String> producer;
    private String topic;

    public DialogHendelseProducer() {
    }

    @Inject
    public DialogHendelseProducer(@KonfigVerdi("kafka.inntektsmelding.innsendinghendelse.topic") String topic,
                                  @KonfigVerdi("KAFKA_BROKERS") String bootstrapServers,
                                  @KonfigVerdi("KAFKA_TRUSTSTORE_PATH") String trustStorePath,
                                  @KonfigVerdi("KAFKA_KEYSTORE_PATH") String keyStoreLocation,
                                  @KonfigVerdi("KAFKA_CREDSTORE_PASSWORD") String credStorePassword) {
        this.topic = topic;
        this.producer = new KafkaProducer<>(getProperties(bootstrapServers, trustStorePath, keyStoreLocation, credStorePassword));
    }

    public void sendJsonMedNøkkel(String nøkkel, String json) {
        var callId = MDCOperations.getCallId() != null ? MDCOperations.getCallId() : MDCOperations.generateCallId();
        var producerRecord = new ProducerRecord<>(topic, nøkkel, json);
        producerRecord.headers().add(MDCOperations.NAV_CALL_ID, callId.getBytes());
        LOG.info("Sender inntektsmelding på topic={}", topic);
        runProducerWithSingleJson(new ProducerRecord<>(topic, nøkkel, json));
    }

    private void runProducerWithSingleJson(ProducerRecord<String, String> record) {
        try {
            var recordMetadata = producer.send(record).get();
            LOG.info("Sendte melding til {}, partition {}, offset {}", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw kafkaPubliseringException(e);
        } catch (Exception e) {
            throw kafkaPubliseringException(e);
        }
    }

    private IntegrasjonException kafkaPubliseringException(Exception e) {
        return new IntegrasjonException("FP-HENDELSE-925475", "Uventet feil ved sending til Kafka, topic " + topic, e);
    }

    private Properties getProperties(String bootstrapServers,
                                     String trustStorePath,
                                     String keyStoreLocation,
                                     String credStorePassword) {
        var clientId = "KP-fpsak";

        final Properties props = new Properties();
        props.put(CommonClientConfigs.CLIENT_ID_CONFIG, clientId);
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        if (IS_DEPLOYMENT) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name);
            props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
            props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks");
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStorePath);
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, credStorePassword);
            props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
            props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keyStoreLocation);
            props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, credStorePassword);
            props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, credStorePassword);
        } else {
            props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_SSL.name);
            props.setProperty(SaslConfigs.SASL_MECHANISM, "PLAIN");
            String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            String jaasCfg = String.format(jaasTemplate, "vtp", "vtp");
            props.setProperty(SaslConfigs.SASL_JAAS_CONFIG, jaasCfg);
        }

        // Serde
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return props;
    }

}
