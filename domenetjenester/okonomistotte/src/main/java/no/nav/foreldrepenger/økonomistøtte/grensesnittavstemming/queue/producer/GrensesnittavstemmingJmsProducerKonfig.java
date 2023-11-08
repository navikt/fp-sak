package no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming.queue.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;

import no.nav.foreldrepenger.felles.jms.JmsKonfig;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.økonomistøtte.queue.config.FellesJmsKonfig;

@ApplicationScoped
public class GrensesnittavstemmingJmsProducerKonfig extends FellesJmsKonfig {

    GrensesnittavstemmingJmsProducerKonfig() {
    }

    @Inject
    public GrensesnittavstemmingJmsProducerKonfig(@KonfigVerdi("mq.username") String bruker,
                                                  @KonfigVerdi("mq.password") String passord,
                                                  @KonfigVerdi("mqGateway02.hostname") String host,
                                                  @KonfigVerdi("mqGateway02.port") int port,
                                                  @KonfigVerdi("mqGateway02.name") String managerName,
                                                  @KonfigVerdi(value = "mqGateway02.channel", required = false) String channel,
                                                  @KonfigVerdi("ray.avstem.data.queuename") String queueName) throws JMSException {
        this.jmsKonfig = new JmsKonfig(host, port, managerName, channel, bruker, passord, queueName, null);
        this.mqConnectionFactory = settOppConnectionFactory(host, port, channel, managerName);
        this.mqQueue = settOppMessageQueue(queueName, true);
    }
}
