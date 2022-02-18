package no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming.queue.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.vedtak.felles.integrasjon.jms.ExternalQueueProducer;
import no.nav.vedtak.felles.integrasjon.jms.JmsMessage;

@ApplicationScoped
public class GrensesnittavstemmingJmsProducer extends ExternalQueueProducer {

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
