package no.nav.foreldrepenger.web.app.tjenester;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import no.nav.foreldrepenger.mottak.kabal.KabalHendelseStream;
import no.nav.foreldrepenger.mottak.vedtak.kafka.VedtaksHendelseConsumer;

/**
 * Triggers start of Kafka consum
 */
@WebListener
public class KafkaConsumerStarter implements ServletContextListener {

    @Inject //NOSONAR
    private HistorikkConsumer historikkConsumer; //NOSONAR
    @Inject //NOSONAR
    private VedtaksHendelseConsumer vedtaksHendelseConsumer; //NOSONAR
    @Inject //NOSONAR
    private KabalHendelseStream kabalHendelseConsumer; //NOSONAR

    public KafkaConsumerStarter() { //NOSONAR
        // For CDI
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        historikkConsumer.start();
        vedtaksHendelseConsumer.start();
        kabalHendelseConsumer.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        historikkConsumer.stop();
        vedtaksHendelseConsumer.stop();
        kabalHendelseConsumer.stop();
    }
}
