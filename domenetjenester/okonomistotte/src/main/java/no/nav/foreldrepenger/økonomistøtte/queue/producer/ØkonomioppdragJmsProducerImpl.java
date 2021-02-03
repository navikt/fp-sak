package no.nav.foreldrepenger.økonomistøtte.queue.producer;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.ConnectionFactory;
import javax.jms.Queue;

import no.nav.foreldrepenger.økonomistøtte.queue.Mq;
import no.nav.vedtak.felles.integrasjon.jms.JmsKonfig;
import no.nav.vedtak.felles.integrasjon.jms.JmsMessage;

@ApplicationScoped
@Mq
public class ØkonomioppdragJmsProducerImpl extends ØkonomioppdragJmsProducer {

    public ØkonomioppdragJmsProducerImpl() {
        // CDI
    }

    @Inject
    public ØkonomioppdragJmsProducerImpl(@Named("økonomioppdragjmsproducerkonfig") JmsKonfig konfig) {
        super(konfig);
    }

    @Override
    public void sendØkonomiOppdrag(String oppdragXML) {
        log.info("Sender oppdragXML til Oppdragssystemet");
        sendTextMessage(JmsMessage.builder().withMessage(oppdragXML).build());
    }

    @Override
    @Resource(mappedName = ØkonomioppdragJmsProducerKonfig.JNDI_JMS_CONNECTION_FACTORY)
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        super.setConnectionFactory(connectionFactory);
    }

    @Override
    @Resource(mappedName = ØkonomioppdragJmsProducerKonfig.JNDI_QUEUE)
    public void setQueue(Queue queue) {
        super.setQueue(queue);
    }

}
