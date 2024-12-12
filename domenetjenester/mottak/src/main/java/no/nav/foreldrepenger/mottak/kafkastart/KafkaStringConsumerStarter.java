package no.nav.foreldrepenger.mottak.kafkastart;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.vedtak.server.Controllable;
import no.nav.vedtak.server.LiveAndReadinessAware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.mottak.kabal.KabalHendelseHåndterer;
import no.nav.foreldrepenger.mottak.vedtak.kafka.VedtaksHendelseHåndterer;
import no.nav.foreldrepenger.produksjonsstyring.behandlinghendelse.BehandlingHendelseHåndterer;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaConsumerManager;


@ApplicationScoped
public class KafkaStringConsumerStarter implements LiveAndReadinessAware, Controllable {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaStringConsumerStarter.class);

    private KafkaConsumerManager<String, String> kcm;

    KafkaStringConsumerStarter() {
        // CDI proxy
    }

    @Inject
    public KafkaStringConsumerStarter(VedtaksHendelseHåndterer vedtaksHendelseHåndterer,
                                      BehandlingHendelseHåndterer behandlingHendelseHåndterer,
                                      KabalHendelseHåndterer kabalHendelseHåndterer) {
        // Kan vurdere Any Instance<KafkaStringMessageHandler> som input - husk stream.toList som argument til new KCM
        this.kcm = new KafkaConsumerManager<>(List.of(vedtaksHendelseHåndterer, behandlingHendelseHåndterer, kabalHendelseHåndterer));
    }


    @Override
    public boolean isAlive() {
        return kcm.allRunning();
    }

    @Override
    public boolean isReady() {
        return isAlive();
    }

    @Override
    public void start() {
        LOG.info("Starter konsumering av topics={}", kcm.topicNames());
        kcm.start((t, e) -> LOG.error("{} :: Caught exception in stream, exiting", t, e));
    }

    @Override
    public void stop() {
        LOG.info("Starter shutdown av topics={} med 10 sekunder timeout", kcm.topicNames());
        kcm.stop();
    }
}
