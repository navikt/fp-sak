package no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming.queue.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.felles.jms.JmsMessage;
import no.nav.foreldrepenger.felles.jms.QueueProducer;

@ApplicationScoped
public class GrensesnittavstemmingJmsProducer extends QueueProducer {

    public GrensesnittavstemmingJmsProducer() {
        // CDI
    }

    @Inject
    public GrensesnittavstemmingJmsProducer(GrensesnittavstemmingJmsProducerKonfig konfig) {
        super(konfig.getJmsKonfig());
        super.setConnectionFactory(konfig.getMqConnectionFactory());
        super.setQueue(konfig.getMqQueue());
    }

    public void sendGrensesnittavstemming(String xml) {
        sendTextMessage(JmsMessage.builder().withMessage(xml).build());
    }

}
