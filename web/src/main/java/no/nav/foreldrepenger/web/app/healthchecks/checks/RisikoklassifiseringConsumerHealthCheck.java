package no.nav.foreldrepenger.web.app.healthchecks.checks;

import no.nav.foreldrepenger.domene.risikoklassifisering.konsument.RisikoklassifiseringConsumer;
import org.apache.kafka.streams.KafkaStreams;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
        if (tilstand.isRunning() || KafkaStreams.State.CREATED.equals(tilstand)) {
            intTestRes.setOk(true);
        } else {
            intTestRes.setOk(false);
        }
        intTestRes.noteResponseTime();

        return intTestRes;
    }
}
