package no.nav.foreldrepenger.web.app.healthchecks.checks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.streams.KafkaStreams;

import no.nav.foreldrepenger.historikk.kafka.HistorikkConsumer;

@ApplicationScoped
public class HistorikkConsumerHealthCheck extends ExtHealthCheck {

    private HistorikkConsumer consumer;

    HistorikkConsumerHealthCheck() {
    }

    @Inject
    public HistorikkConsumerHealthCheck(HistorikkConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    protected String getDescription() {
        return "Test av consumering av historikk fra kafka";
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
        intTestRes.setOk(holderPåÅKonsumere(tilstand));
        intTestRes.noteResponseTime();

        return intTestRes;
    }

    private boolean holderPåÅKonsumere(KafkaStreams.State tilstand) {
        return tilstand.isRunning() || KafkaStreams.State.CREATED.equals(tilstand);
    }
}
