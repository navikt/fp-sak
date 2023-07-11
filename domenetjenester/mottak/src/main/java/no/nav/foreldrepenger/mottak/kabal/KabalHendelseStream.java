package no.nav.foreldrepenger.mottak.kabal;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaProperties;
import no.nav.vedtak.log.metrics.Controllable;
import no.nav.vedtak.log.metrics.LiveAndReadinessAware;

/*
 * Dokumentasjon https://github.com/navikt/kabal-api/tree/main/docs/integrasjon
 */
@ApplicationScoped
public class KabalHendelseStream implements LiveAndReadinessAware, Controllable {

    private static final Logger LOG = LoggerFactory.getLogger(KabalHendelseStream.class);
    private static final String APPLICATION_ID = "fpsak"; // Hold konstant pga offset commit !!
    private static final Environment ENV = Environment.current();
    private static final boolean IS_DEPLOY = ENV.isProd() || ENV.isDev();

    private KafkaStreams stream;
    private String topicName;

    KabalHendelseStream() {
    }

    @Inject
    public KabalHendelseStream(@KonfigVerdi(value = "kafka.kabal.topic", defaultVerdi = "klage.behandling-events.v1") String topicName,
                               KabalHendelseHåndterer kabalHendelseHåndterer) {
        this.topicName = topicName;

        final Consumed<String, String> consumed = Consumed.with(Topology.AutoOffsetReset.EARLIEST);

        var builder = new StreamsBuilder();
        builder.stream(topicName, consumed)
            .foreach(kabalHendelseHåndterer::handleMessage);

        this.stream = new KafkaStreams(builder.build(), KafkaProperties.forStreamsStringValue(APPLICATION_ID));
    }


    private void addShutdownHooks() {
        stream.setStateListener((newState, oldState) -> {
            LOG.info("{} :: From state={} to state={}", getTopicName(), oldState, newState);

            if (newState == KafkaStreams.State.ERROR) {
                // if the stream has died there is no reason to keep spinning
                LOG.warn("{} :: No reason to keep living, closing stream", getTopicName());
                stop();
            }
        });
        stream.setUncaughtExceptionHandler((t, e) -> {
            LOG.error("{} :: Caught exception in stream, exiting", getTopicName(), e);
            stop();
        });
    }

    @Override
    public void start() {
        if (!IS_DEPLOY) return;
        addShutdownHooks();

        stream.start();
        LOG.info("Starter konsumering av topic={}, tilstand={}", getTopicName(), stream.state());
    }

    private String getTopicName() {
        return topicName;
    }

    @Override
    public boolean isAlive() {
        return !IS_DEPLOY || (stream != null && stream.state().isRunningOrRebalancing());
    }

    @Override
    public boolean isReady() {
        return isAlive();
    }

    @Override
    public void stop() {
        if (!IS_DEPLOY) return;
        LOG.info("Starter shutdown av topic={}, tilstand={} med 15 sekunder timeout", getTopicName(), stream.state());
        stream.close(Duration.ofSeconds(15));
        LOG.info("Shutdown av topic={}, tilstand={} med 15 sekunder timeout", getTopicName(), stream.state());
    }
}
