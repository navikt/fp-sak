package no.nav.foreldrepenger.domene.risikoklassifisering.konsument;

import no.nav.vedtak.apptjeneste.AppServiceHandler;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.Properties;

@ApplicationScoped
public class RisikoklassifiseringConsumer implements AppServiceHandler {

    private static final Logger log = LoggerFactory.getLogger(RisikoklassifiseringConsumer.class);

    private String topic;
    private KafkaStreams stream;

    RisikoklassifiseringConsumer() {
    }

    @Inject
    public RisikoklassifiseringConsumer(RisikoklassifiseringStreamKafkaProperties streamProperties,
                                        RisikoklassifiseringMeldingsHåndterer meldingsHåndterer) {
        this.topic = streamProperties.getTopic();

        Properties props = setupProperties(streamProperties);

        final StreamsBuilder builder = new StreamsBuilder();

        Consumed<String, String> stringStringConsumed = Consumed.with(Topology.AutoOffsetReset.EARLIEST);
        builder.stream(this.topic, stringStringConsumed)
            .foreach((header, payload) -> meldingsHåndterer.lagreMelding(payload));

        final Topology topology = builder.build();
        stream = new KafkaStreams(topology, props);
    }

    private Properties setupProperties(RisikoklassifiseringStreamKafkaProperties streamProperties) {
        Properties props = new Properties();

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, streamProperties.getApplicationId());
        log.info("Bruker applicationID: " + streamProperties.getApplicationId());
        props.put(StreamsConfig.CLIENT_ID_CONFIG, streamProperties.getClientId());
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, streamProperties.getBootstrapServers());

        // Sikkerhet
        if (streamProperties.harSattBrukernavn()) {
            log.info("Using user name {} to authenticate against Kafka brokers ", streamProperties.getUsername());
            props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
            String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            props.put(SaslConfigs.SASL_JAAS_CONFIG, String.format(jaasTemplate, streamProperties.getUsername(), streamProperties.getPassword()));
        }

        // Serde
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, streamProperties.getKeyClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, streamProperties.getValueClass());
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler.class);

        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, "at_least_once");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "100000");

        return props;
    }

    @Override
    public void start() {
        addShutdownHooks();
        stream.start();
        log.info("Starter konsumering av topic={}, tilstand={}", topic, stream.state());
    }

    @Override
    public void stop() {
        log.info("Starter shutdown av topic={}, tilstand={} med 10 sekunder timeout", topic, stream.state());
        stream.close(Duration.ofSeconds(10));
        log.info("Shutdown av topic={}, tilstand={} med 10 sekunder timeout", topic, stream.state());
    }

    public KafkaStreams.State getTilstand() {
        return stream.state();
    }

    public String getTopic() {
        return topic;
    }

    private void addShutdownHooks() {
        stream.setStateListener((newState, oldState) -> {
            log.info("From state={} to state={}", oldState, newState);

            if (newState == KafkaStreams.State.ERROR) {
                // if the stream has died there is no reason to keep spinning
                log.warn("No reason to keep living, closing stream");
                stop();
            }
        });
        stream.setUncaughtExceptionHandler((t, e) -> {
            log.error("Caught exception in stream, exiting", e);
            stop();
        });
    }

}
