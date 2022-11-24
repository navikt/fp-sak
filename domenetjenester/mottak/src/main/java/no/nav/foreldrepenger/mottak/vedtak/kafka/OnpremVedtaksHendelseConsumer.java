package no.nav.foreldrepenger.mottak.vedtak.kafka;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.liveness.KafkaIntegration;
import no.nav.vedtak.apptjeneste.AppServiceHandler;

@ApplicationScoped
public class OnpremVedtaksHendelseConsumer implements AppServiceHandler, KafkaIntegration {

    private static final Logger LOG = LoggerFactory.getLogger(OnpremVedtaksHendelseConsumer.class);
    private KafkaStreams stream;
    private String topic;

    OnpremVedtaksHendelseConsumer() {
    }

    @Inject
    public OnpremVedtaksHendelseConsumer(VedtaksHendelseHåndterer vedtaksHendelseHåndterer, OnpremVedtakStreamKafkaProperties streamKafkaProperties) {
        this.topic = streamKafkaProperties.getTopic();

        var props = setupProperties(streamKafkaProperties);

        final var builder = new StreamsBuilder();

        Consumed<String, String> stringStringConsumed = Consumed.with(Topology.AutoOffsetReset.LATEST);
        builder.stream(this.topic, stringStringConsumed)
            .foreach(vedtaksHendelseHåndterer::handleMessage);

        final var topology = builder.build();
        stream = new KafkaStreams(topology, props);
    }

    private void addShutdownHooks() {
        stream.setStateListener((newState, oldState) -> {
            LOG.info("{} :: From state={} to state={}", topic, oldState, newState);

            if (newState == KafkaStreams.State.ERROR) {
                // if the stream has died there is no reason to keep spinning
                LOG.warn("{} :: No reason to keep living, closing stream", topic);
                stop();
            }
        });
        stream.setUncaughtExceptionHandler((t, e) -> {
            LOG.error("{} :: Caught exception in stream, exiting", topic, e);
            stop();
        });
    }

    private Properties setupProperties(OnpremVedtakStreamKafkaProperties streamProperties) {
        var props = new Properties();

        LOG.info("Consuming topic='{}' with applicationId='{}'", streamProperties.getTopic(), streamProperties.getApplicationId());
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, streamProperties.getApplicationId());
        props.put(StreamsConfig.CLIENT_ID_CONFIG, streamProperties.getClientId());
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, streamProperties.getBootstrapServers());

        // Sikkerhet
        if (streamProperties.harSattBrukernavn()) {
            LOG.info("Using user name {} to authenticate against Kafka brokers ", streamProperties.getUsername());
            props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
            var jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            props.put(SaslConfigs.SASL_JAAS_CONFIG, String.format(jaasTemplate, streamProperties.getUsername(), streamProperties.getPassword()));
        }

        // Setup truststore? Skal det settes opp?
        if (streamProperties.harSattTrustStore()) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, streamProperties.getTrustStorePath());
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, streamProperties.getTrustStorePassword());
        }

        // Setup schema-registry
        if (streamProperties.getSchemaRegistryUrl() != null) {
            props.put("schema.registry.url", streamProperties.getSchemaRegistryUrl());
        }

        // Serde
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, streamProperties.getKeyClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, streamProperties.getValueClass());
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler.class);

        // Polling
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "200");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "100000");

        return props;
    }

    @Override
    public void start() {
        addShutdownHooks();

        stream.start();
        LOG.info("Starter konsumering av topic={}, tilstand={}", topic, stream.state());
    }

    public KafkaStreams.State getTilstand() {
        return stream.state();
    }


    public String getTopic() {
        return topic;
    }

    @Override
    public void stop() {
        LOG.info("Starter shutdown av topic={}, tilstand={} med 10 sekunder timeout", topic, stream.state());
        stream.close(Duration.of(20, ChronoUnit.SECONDS));
        LOG.info("Shutdown av topic={}, tilstand={} med 10 sekunder timeout", topic, stream.state());
    }

    @Override
    public boolean isAlive() {
        return stream != null && stream.state().isRunningOrRebalancing();
    }
}
