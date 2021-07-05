package no.nav.foreldrepenger.web.server.jetty;

import javax.jms.JMSException;
import javax.naming.NamingException;

import org.eclipse.jetty.plus.jndi.EnvEntry;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.mq.jms.MQQueue;
import com.ibm.msg.client.jms.JmsConstants;
import com.ibm.msg.client.wmq.WMQConstants;
import com.ibm.msg.client.wmq.compat.jms.internal.JMSC;

import no.nav.foreldrepenger.konfig.Environment;

class JmsKonfig {

    private static final Environment ENV = Environment.current();

    private static final int MQ_TARGET_CLIENT = WMQConstants.WMQ_MESSAGE_BODY_MQ;

    private JmsKonfig() { // Util class
    }

    static void
    settOppJndiConnectionfactory(String jndiName) throws JMSException, NamingException {
        var mqConnectionFactory = createConnectionfactory(
                ENV.getProperty("mqGateway02.hostname"),
                ENV.getProperty("mqGateway02.port", Integer.class),
                ENV.getProperty("mqGateway02.channel"),
                ENV.getProperty("mqGateway02.name"));
        new EnvEntry(jndiName, mqConnectionFactory);
    }

    static void settOppJndiMessageQueue(String jndiName, String queueNameProp) throws NamingException, JMSException {
        settOppJndiMessageQueue(jndiName, queueNameProp, false);
    }

    static void settOppJndiMessageQueue(String jndiName, String queueNameProp, boolean mqTargetClient) throws NamingException, JMSException {
        var queue = new MQQueue(ENV.getProperty(queueNameProp));
        if (mqTargetClient) {
            queue.setMessageBodyStyle(MQ_TARGET_CLIENT);
        }
        new EnvEntry(jndiName, queue);
    }

    private static MQConnectionFactory createConnectionfactory(String hostName, Integer port, String channel, String queueManagerName)
            throws JMSException {
        var connectionFactory = new MQConnectionFactory();
        connectionFactory.setHostName(hostName);
        connectionFactory.setPort(port);
        connectionFactory.setQueueManager(queueManagerName);
        if (channel != null) { // TODO: Må fikses det trenges ikke ved lokalt kjøring og vtp da mq mockes.
            connectionFactory.setChannel(channel);
        }
        connectionFactory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
        connectionFactory.setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true);

        return connectionFactory;
    }
}
