package no.nav.foreldrepenger.web.app.healthchecks.checks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.streams.KafkaStreams;

import no.nav.foreldrepenger.domene.risikoklassifisering.konsument.RisikoklassifiseringConsumer;

@ApplicationScoped
public class RisikoklassifiseringConsumerHealthCheck extends ExtHealthCheck {

    private RisikoklassifiseringConsumer consumer;

    RisikoklassifiseringConsumerHealthCheck() {
    }

    @Inject
    public RisikoklassifiseringConsumerHealthCheck(RisikoklassifiseringConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    protected String getDescription() {
        return "Test av consumering av risikoklassifisering fra kafka";
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
