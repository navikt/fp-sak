package no.nav.foreldrepenger.økonomistøtte.queue.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.økonomistøtte.queue.Mq;
import no.nav.foreldrepenger.felles.jms.JmsMessage;

@ApplicationScoped
@Mq
public class ØkonomioppdragJmsProducerImpl extends ØkonomioppdragJmsProducer {

    public ØkonomioppdragJmsProducerImpl() {
        // CDI
    }

    @Inject
    public ØkonomioppdragJmsProducerImpl(ØkonomioppdragJmsProducerKonfig konfig) {
        super(konfig.getJmsKonfig());
        super.setConnectionFactory(konfig.getMqConnectionFactory());
        super.setQueue(konfig.getMqQueue());
    }

    @Override
    public void sendØkonomiOppdrag(String oppdragXML) {
        log.info("Sender oppdragXML til Oppdragssystemet");
        sendTextMessage(JmsMessage.builder().withMessage(oppdragXML).build());
    }
}
