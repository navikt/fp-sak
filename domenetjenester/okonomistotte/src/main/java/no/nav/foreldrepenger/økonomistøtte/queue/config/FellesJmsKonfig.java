package no.nav.foreldrepenger.økonomistøtte.queue.config;

import javax.jms.JMSException;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.mq.jms.MQQueue;
import com.ibm.msg.client.jms.JmsConstants;
import com.ibm.msg.client.wmq.WMQConstants;
import com.ibm.msg.client.wmq.compat.jms.internal.JMSC;

import no.nav.vedtak.felles.integrasjon.jms.JmsKonfig;

public class FellesJmsKonfig {

    protected JmsKonfig jmsKonfig;
    protected MQConnectionFactory mqConnectionFactory;
    protected MQQueue mqQueue;

    private static final int MQ_TARGET_CLIENT = WMQConstants.WMQ_MESSAGE_BODY_MQ;

    protected static MQQueue settOppMessageQueue(String queueName) throws JMSException {
        return settOppMessageQueue(queueName, false);
    }

    protected static MQQueue settOppMessageQueue(String queueName, boolean mqTargetClient) throws JMSException {
        var queue = new MQQueue(queueName);
        if (mqTargetClient) {
            queue.setMessageBodyStyle(MQ_TARGET_CLIENT);
        }
        return queue;
    }

    protected static MQConnectionFactory settOppConnectionFactory(String host, int port, String channel, String manager) throws JMSException {
        return createConnectionFactory(host, port, channel, manager);
    }

    private static MQConnectionFactory createConnectionFactory(String hostName, Integer port, String channel, String queueManagerName) throws JMSException {
        MQConnectionFactory connectionFactory = new MQConnectionFactory();
        connectionFactory.setHostName(hostName);
        connectionFactory.setPort(port);
        if (channel != null) {
            connectionFactory.setChannel(channel);
        }
        connectionFactory.setQueueManager(queueManagerName);

        connectionFactory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
        connectionFactory.setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true);

        return connectionFactory;
    }

    public MQConnectionFactory getMqConnectionFactory() {
        return mqConnectionFactory;
    }

    public MQQueue getMqQueue() {
        return mqQueue;
    }

    public JmsKonfig getJmsKonfig() {
        return jmsKonfig;
    }
}
