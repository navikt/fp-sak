package no.nav.foreldrepenger.web.app.healthchecks.checks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.økonomi.grensesnittavstemming.queue.producer.GrensesnittavstemmingJmsProducer;

@ApplicationScoped
public class GrensesnittavstemmingQueueHealthCheck extends QueueHealthCheck {

    GrensesnittavstemmingQueueHealthCheck() {
        // for CDI proxy
    }

    @Inject
    public GrensesnittavstemmingQueueHealthCheck(GrensesnittavstemmingJmsProducer client) {
        super(client);
    }

    @Override
    protected String getDescriptionSuffix() {
        return "Grensesnittavstemming";
    }
}
