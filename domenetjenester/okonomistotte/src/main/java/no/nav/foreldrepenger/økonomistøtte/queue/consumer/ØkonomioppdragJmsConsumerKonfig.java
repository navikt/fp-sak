package no.nav.foreldrepenger.økonomistøtte.queue.consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import jakarta.jms.JMSException;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.økonomistøtte.queue.config.FellesJmsKonfig;
import no.nav.foreldrepenger.felles.jms.JmsKonfig;

@ApplicationScoped
public class ØkonomioppdragJmsConsumerKonfig extends FellesJmsKonfig {

    ØkonomioppdragJmsConsumerKonfig() {
    }

    @Inject
    public ØkonomioppdragJmsConsumerKonfig(@KonfigVerdi("mq.username") String bruker,
                                           @KonfigVerdi("mq.password") String passord,
                                           @KonfigVerdi("mqGateway02.hostname") String host,
                                           @KonfigVerdi("mqGateway02.port") int port,
                                           @KonfigVerdi("mqGateway02.name") String managerName,
                                           @KonfigVerdi(value = "mqGateway02.channel", required = false) String channel,
                                           @KonfigVerdi("fpsak.okonomi.oppdrag.mottak.queuename") String queueName) throws JMSException {
        this.jmsKonfig = new JmsKonfig(host, port, managerName, channel, bruker, passord, queueName, null);
        this.mqConnectionFactory = settOppConnectionFactory(host, port, channel, managerName);
        this.mqQueue = settOppMessageQueue(queueName);
    }
}
