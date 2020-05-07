package no.nav.foreldrepenger.web.app.healthchecks.checks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.streams.KafkaStreams;

import no.nav.foreldrepenger.mottak.vedtak.kafka.VedtaksHendelseConsumer;

@ApplicationScoped
public class VedtaksHendelseConsumerHealthCheck extends ExtHealthCheck {

    private VedtaksHendelseConsumer consumer;

    VedtaksHendelseConsumerHealthCheck() {
    }

    @Inject
    public VedtaksHendelseConsumerHealthCheck(VedtaksHendelseConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    protected String getDescription() {
        return "Test av konsumering av fattetVedtak fra kafka";
    }

    @Override
    protected String getEndpoint() {
        return consumer.getTopic();
    }

    @Override
    protected InternalResult performCheck() {
        InternalResult intTestRes = new InternalResult();

        KafkaStreams.State tilstand = consumer.getTilstand();
        intTestRes.setMessage("Consumer is in state [" + tilstand.name() + "].");
        intTestRes.setOk(tilstand.isRunningOrRebalancing() || KafkaStreams.State.CREATED.equals(tilstand));
        intTestRes.noteResponseTime();

        return intTestRes;
    }
}
