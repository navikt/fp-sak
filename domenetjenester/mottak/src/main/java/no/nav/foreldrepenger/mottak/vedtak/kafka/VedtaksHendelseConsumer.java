package no.nav.foreldrepenger.mottak.vedtak.kafka;

import no.nav.foreldrepenger.domene.liveness.KafkaIntegration;
import no.nav.vedtak.apptjeneste.AppServiceHandler;
import no.nav.vedtak.log.metrics.LivenessAware;
import no.nav.vedtak.log.metrics.ReadinessAware;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.time.Duration;

@ApplicationScoped
public class VedtaksHendelseConsumer implements LivenessAware, ReadinessAware, AppServiceHandler, KafkaIntegration {

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksHendelseConsumer.class);
    private String topicName;
    private KafkaStreams stream;

    public VedtaksHendelseConsumer() {
    }

    @Inject
    public VedtaksHendelseConsumer(VedtakStreamKafkaProperties vedtakStreamKafkaProperties,
                                   VedtaksHendelseHåndterer vedtaksHendelseHåndterer) {
        this.topicName = vedtakStreamKafkaProperties.getTopicName();

        final Consumed<String, String> consumed = Consumed.with(Topology.AutoOffsetReset.EARLIEST);

        final StreamsBuilder builder = new StreamsBuilder();
        builder.stream(topicName, consumed)
            .foreach(vedtaksHendelseHåndterer::handleMessage);

        this.stream = new KafkaStreams(builder.build(), vedtakStreamKafkaProperties.getProperties());
    }

    @Override
    public void start() {
        addShutdownHooks();
        stream.start();
        LOG.info("Starter konsumering av topic={}, tilstand={}", getTopicName(), stream.state());
    }

    @Override
    public void stop() {
        LOG.info("Starter shutdown av topic={}, tilstand={} med 15 sekunder timeout", getTopicName(), stream.state());
        stream.close(Duration.ofSeconds(15));
        LOG.info("Shutdown av topic={}, tilstand={} med 15 sekunder timeout", getTopicName(), stream.state());
    }

    @Override
    public boolean isAlive() {
        return (stream != null && stream.state().isRunningOrRebalancing());
    }

    @Override
    public boolean isReady() {
        return isAlive();
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

    private String getTopicName() {
        return topicName;
    }
}
