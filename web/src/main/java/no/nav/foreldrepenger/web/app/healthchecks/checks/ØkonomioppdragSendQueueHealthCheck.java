package no.nav.foreldrepenger.web.app.healthchecks.checks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.vedtak.felles.integrasjon.okonomistottejms.ØkonomioppdragJmsProducer;


@ApplicationScoped
public class ØkonomioppdragSendQueueHealthCheck extends QueueHealthCheck {

    ØkonomioppdragSendQueueHealthCheck() {
        // for CDI proxy
    }

    @Inject
    public ØkonomioppdragSendQueueHealthCheck(ØkonomioppdragJmsProducer client) {
        super(client);
    }

    @Override
    protected String getDescriptionSuffix() {
        return "Økonomioppdrag Send";
    }
}
