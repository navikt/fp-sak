package no.nav.foreldrepenger.web.app.healthchecks.checks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.mq.SakOgBehandlingClient;

@ApplicationScoped
public class SakOgBehandlingQueueHealthCheck extends QueueHealthCheck {

    SakOgBehandlingQueueHealthCheck() {
        // for CDI proxy
    }

    @Inject
    public SakOgBehandlingQueueHealthCheck(SakOgBehandlingClient client) {
        super(client);
    }

    @Override
    protected String getDescriptionSuffix() {
        return "Sak og behandling hendelse";
    }
}
