package no.nav.foreldrepenger.web.app.healthchecks.checks;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.JMSRuntimeException;

import no.nav.vedtak.server.LiveAndReadinessAware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.felles.jms.QueueSelftest;
import no.nav.foreldrepenger.økonomistøtte.queue.consumer.ØkonomiOppdragKvitteringAsyncJmsConsumer;

@ApplicationScoped
public class KvitteringQueueHealthCheck implements LiveAndReadinessAware {
    private static final Logger LOG = LoggerFactory.getLogger(KvitteringQueueHealthCheck.class);
    private QueueSelftest client;

    KvitteringQueueHealthCheck() {
        // for CDI proxy
    }

    @Inject
    public KvitteringQueueHealthCheck(ØkonomiOppdragKvitteringAsyncJmsConsumer kravgrunnlagAsyncJmsConsumer) {
        this.client = kravgrunnlagAsyncJmsConsumer;
    }

    private boolean isOK() {
        try {
            client.testConnection();
        } catch (JMSRuntimeException | JMSException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Feil ved Kvittering meldingskø helsesjekk: {}", client.getConnectionEndpoint());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isReady() {
        return isOK();
    }

    @Override
    public boolean isAlive() {
        return isOK();
    }
}
