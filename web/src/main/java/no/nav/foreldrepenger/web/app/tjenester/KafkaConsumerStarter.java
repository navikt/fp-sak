package no.nav.foreldrepenger.web.app.tjenester;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import no.nav.foreldrepenger.mottak.kabal.KabalHendelseStream;
import no.nav.foreldrepenger.mottak.vedtak.kafka.OnpremVedtaksHendelseConsumer;
import no.nav.foreldrepenger.mottak.vedtak.kafka.VedtaksHendelseConsumer;

/**
 * Triggers start of Kafka consum
 */
@WebListener
public class KafkaConsumerStarter implements ServletContextListener {

    @Inject //NOSONAR
    private OnpremVedtaksHendelseConsumer onpremVedtaksHendelseConsumer; //NOSONAR
    @Inject //NOSONAR
    private KabalHendelseStream kabalHendelseConsumer; //NOSONAR

    @Inject //NOSONAR
    private VedtaksHendelseConsumer vedtaksHendelseConsumer; //NOSONAR

    public KafkaConsumerStarter() { //NOSONAR
        // For CDI
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        onpremVedtaksHendelseConsumer.start();
        vedtaksHendelseConsumer.start();
        kabalHendelseConsumer.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        onpremVedtaksHendelseConsumer.stop();
        vedtaksHendelseConsumer.stop();
        kabalHendelseConsumer.stop();
    }
}
