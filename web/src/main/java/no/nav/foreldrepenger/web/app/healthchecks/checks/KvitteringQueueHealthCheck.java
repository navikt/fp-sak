package no.nav.foreldrepenger.web.app.healthchecks.checks;

import jakarta.jms.JMSException;
import jakarta.jms.JMSRuntimeException;
import no.nav.foreldrepenger.økonomistøtte.queue.consumer.ØkonomiOppdragKvitteringAsyncJmsConsumer;
import no.nav.foreldrepenger.felles.jms.QueueSelftest;
import no.nav.vedtak.log.metrics.LiveAndReadinessAware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
        } catch (JMSRuntimeException | JMSException e) { //NOSONAR
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
