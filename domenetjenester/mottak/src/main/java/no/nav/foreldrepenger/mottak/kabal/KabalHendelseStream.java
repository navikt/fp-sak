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

import no.nav.foreldrepenger.domene.liveness.KafkaIntegration;
import no.nav.vedtak.apptjeneste.AppServiceHandler;
import no.nav.vedtak.log.metrics.LivenessAware;
import no.nav.vedtak.log.metrics.ReadinessAware;

/*
 * Dokumentasjon https://github.com/navikt/kabal-api/tree/main/docs/integrasjon
 */
@ApplicationScoped
public class KabalHendelseStream implements LivenessAware, ReadinessAware, AppServiceHandler, KafkaIntegration {

    private static final Logger LOG = LoggerFactory.getLogger(KabalHendelseStream.class);

    private KafkaStreams stream;
    private String topicName;
    private boolean isDeployment;

    KabalHendelseStream() {
    }

    @Inject
    public KabalHendelseStream(KabalHendelseHåndterer kabalHendelseHåndterer,
                               KabalHendelseProperties streamKafkaProperties) {
        this.topicName = streamKafkaProperties.getTopicName();
        this.isDeployment = streamKafkaProperties.isDeployment();

        final Consumed<String, String> consumed = Consumed.with(Topology.AutoOffsetReset.EARLIEST);

        final StreamsBuilder builder = new StreamsBuilder();
        builder.stream(topicName, consumed)
            .foreach(kabalHendelseHåndterer::handleMessage);

        this.stream = new KafkaStreams(builder.build(), streamKafkaProperties.getProperties());
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
        if (!isDeployment) return;
        addShutdownHooks();

        stream.start();
        LOG.info("Starter konsumering av topic={}, tilstand={}", getTopicName(), stream.state());
    }

    private String getTopicName() {
        return topicName;
    }

    @Override
    public boolean isAlive() {
        return true; //(stream != null) && stream.state().isRunningOrRebalancing();
    }

    @Override
    public boolean isReady() {
        return isAlive();
    }

    @Override
    public void stop() {
        if (!isDeployment) return;
        LOG.info("Starter shutdown av topic={}, tilstand={} med 15 sekunder timeout", getTopicName(), stream.state());
        stream.close(Duration.ofSeconds(15));
        LOG.info("Shutdown av topic={}, tilstand={} med 15 sekunder timeout", getTopicName(), stream.state());
    }
}
