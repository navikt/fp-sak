package no.nav.foreldrepenger.web.app.healthchecks.checks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.økonomi.økonomistøtte.queue.consumer.ØkonomioppdragAsyncJmsConsumer;

@ApplicationScoped
public class ØkonomioppdragMottakQueueHealthCheck extends QueueHealthCheck {

    ØkonomioppdragMottakQueueHealthCheck() {
        // for CDI proxy
    }

    @Inject
    public ØkonomioppdragMottakQueueHealthCheck(ØkonomioppdragAsyncJmsConsumer client) {
        super(client);
    }

    @Override
    protected String getDescriptionSuffix() {
        return "Økonomioppdrag Mottak";
    }
}
