package no.nav.foreldrepenger.mottak.vedtak.kafka;


import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import java.util.Properties;
import java.util.UUID;

@Dependent
public class VedtakStreamKafkaProperties {

    private static final Logger LOG = LoggerFactory.getLogger(VedtakStreamKafkaProperties.class);
    private static final Environment ENV = Environment.current();
    private final boolean isDeployment = ENV.isProd() || ENV.isDev();
    private final String trustStorePath;
    private final String keyStoreLocation;
    private final String credStorePassword;
    private final String applicationId;
    private final String clientId;
    private final String bootstrapServers;
    private final String topicName;


    @Inject
    public VedtakStreamKafkaProperties(@KonfigVerdi("kafka.fattevedtak.aiven.topic") String topicName,
                                       @KonfigVerdi("KAFKA_BROKERS") String bootstrapServers,
                                       @KonfigVerdi("KAFKA_TRUSTSTORE_PATH") String trustStorePath,
                                       @KonfigVerdi("KAFKA_KEYSTORE_PATH") String keyStoreLocation,
                                       @KonfigVerdi("KAFKA_CREDSTORE_PASSWORD") String credStorePassword) {
        this.trustStorePath = trustStorePath;
        this.keyStoreLocation = keyStoreLocation;
        this.credStorePassword = credStorePassword;
        this.applicationId = "fpsak";
        this.clientId = "fpsak-" + UUID.randomUUID();
        this.bootstrapServers = bootstrapServers;
        this.topicName = topicName;
    }

    public Properties getProperties() {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        LOG.info("Stream APPLICATION_ID_CONFIG: {}", props.getProperty(StreamsConfig.APPLICATION_ID_CONFIG));
        props.put(StreamsConfig.CLIENT_ID_CONFIG, clientId);
        LOG.info("Stream CLIENT_ID_CONFIG: {}", props.getProperty(StreamsConfig.CLIENT_ID_CONFIG));
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        if (isDeployment) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name);
            props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
            props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks");
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStorePath);
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, credStorePassword);
            props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
            props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keyStoreLocation);
            props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, credStorePassword);
        } else {
            props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_SSL.name);
            props.setProperty(SaslConfigs.SASL_MECHANISM, "PLAIN");
            String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            String jaasCfg = String.format(jaasTemplate, "vtp", "vtp");
            props.setProperty(SaslConfigs.SASL_JAAS_CONFIG, jaasCfg);
        }

        // Serde
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler.class);

        // Polling
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "200");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "60000");

        return props;
    }

    public String getTopicName() {
        return topicName;
    }

    public boolean isDeployment() {
        return isDeployment;
    }

}
