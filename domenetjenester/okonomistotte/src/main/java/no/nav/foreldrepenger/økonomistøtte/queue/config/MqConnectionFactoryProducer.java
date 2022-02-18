package no.nav.foreldrepenger.økonomistøtte.queue.config;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.JMSException;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.jms.JmsConstants;
import com.ibm.msg.client.wmq.WMQConstants;
import com.ibm.msg.client.wmq.compat.jms.internal.JMSC;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
public class MqConnectionFactoryProducer {

    private MQConnectionFactory mqConnectionFactory;

    MqConnectionFactoryProducer() {
    }

    @Inject
    public MqConnectionFactoryProducer(@KonfigVerdi("mqGateway02.hostname") String host,
                                       @KonfigVerdi("mqGateway02.port") int port,
                                       @KonfigVerdi("mqGateway02.name") String managerName,
                                       @KonfigVerdi(value = "mqGateway02.channel", required = false) String channel) throws JMSException {
        this.mqConnectionFactory = createConnectionfactory(host, port, channel, managerName);
    }

    private static MQConnectionFactory createConnectionfactory(String hostName,
                                                               Integer port,
                                                               String channel,
                                                               String queueManagerName) throws JMSException {
        var connectionFactory = new MQConnectionFactory();
        connectionFactory.setHostName(hostName);
        connectionFactory.setPort(port);
        connectionFactory.setQueueManager(queueManagerName);
        if (channel != null) {
            connectionFactory.setChannel(channel);
        }
        connectionFactory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
        connectionFactory.setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true);

        return connectionFactory;
    }

    public MQConnectionFactory getMqConnectionFactory() {
        return mqConnectionFactory;
    }
}
