package no.nav.foreldrepenger.økonomistøtte.queue.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import jakarta.jms.JMSException;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.økonomistøtte.queue.config.FellesJmsKonfig;
import no.nav.vedtak.felles.integrasjon.jms.JmsKonfig;

@ApplicationScoped
public class ØkonomioppdragJmsProducerKonfig extends FellesJmsKonfig {

    ØkonomioppdragJmsProducerKonfig() {
    }

    @Inject
    public ØkonomioppdragJmsProducerKonfig(@KonfigVerdi("mq.username") String bruker,
                                           @KonfigVerdi("mq.password") String passord,
                                           @KonfigVerdi("mqGateway02.hostname") String host,
                                           @KonfigVerdi("mqGateway02.port") int port,
                                           @KonfigVerdi("mqGateway02.name") String managerName,
                                           @KonfigVerdi(value = "mqGateway02.channel", required = false) String channel,
                                           @KonfigVerdi("fpsak.okonomi.oppdrag.send.queuename") String queueName,
                                           @KonfigVerdi("fpsak.okonomi.oppdrag.mottak.queuename") String kvitteringQueueName) throws JMSException {
        this.jmsKonfig = new JmsKonfig(host, port, managerName, channel, bruker, passord, queueName, kvitteringQueueName);
        this.mqConnectionFactory = settOppConnectionFactory(host, port, channel, managerName);
        this.mqQueue = settOppMessageQueue(queueName);
    }
}


