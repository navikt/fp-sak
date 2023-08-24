package no.nav.foreldrepenger.økonomistøtte.queue.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.felles.jms.JmsMessage;
import no.nav.foreldrepenger.økonomistøtte.queue.Mq;

@ApplicationScoped
@Mq
public class ØkonomioppdragJmsProducerImpl extends ØkonomioppdragJmsProducer {

    ØkonomioppdragJmsProducerImpl() {
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
        LOG.info("Sender oppdragXML til Oppdragssystemet");
        sendTextMessage(JmsMessage.builder().withMessage(oppdragXML).build());
    }
}
