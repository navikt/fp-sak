package no.nav.foreldrepenger.web.server.jetty;

import javax.jms.JMSException;
import javax.naming.NamingException;

import org.eclipse.jetty.plus.jndi.EnvEntry;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.mq.jms.MQQueue;
import com.ibm.msg.client.wmq.WMQConstants;
import com.ibm.msg.client.wmq.compat.jms.internal.JMSC;

import no.nav.vedtak.util.env.Environment;

class JmsKonfig {

    private static final Environment ENV = Environment.current();

    private static final int MQ_TARGET_CLIENT = WMQConstants.WMQ_MESSAGE_BODY_MQ;

    private JmsKonfig() { // Util class
    }

    static void settOppJndiConnectionfactory(String jndiName, String queueManagerAlias, String channelAlias) throws JMSException, NamingException {
        var mqConnectionFactory = createConnectionfactory(
                ENV.getProperty(queueManagerAlias + ".hostname"),
                Integer.parseUnsignedInt(ENV.getProperty(queueManagerAlias + ".port")),
                ENV.getProperty(channelAlias + ".name"),
                ENV.getProperty(queueManagerAlias + ".name"),
                Boolean.getBoolean("mqGateway02.useSslOnJetty"));
        new EnvEntry(jndiName, mqConnectionFactory);
    }

    static void settOppJndiMessageQueue(String jndiName, String queueAlias) throws NamingException, JMSException {
        settOppJndiMessageQueue(jndiName, queueAlias, false);
    }

    static void settOppJndiMessageQueue(String jndiName, String queueAlias, boolean mqTargetClient) throws NamingException, JMSException {
        var queue = new MQQueue(ENV.getProperty(queueAlias + ".queueName"));
        if (mqTargetClient) {
            queue.setMessageBodyStyle(MQ_TARGET_CLIENT);
        }
        new EnvEntry(jndiName, queue);
    }

    /**
     * @param useSSL - FIXME (u139158): PFP-1176 Saneres når vi er over til å bruke
     *               SSL mot MQ overalt
     */
    private static MQConnectionFactory createConnectionfactory(String hostName, Integer port, String channel, String queueManagerName, boolean useSSL)
            throws JMSException {
        var connectionFactory = new MQConnectionFactory();
        connectionFactory.setHostName(hostName);
        connectionFactory.setPort(port);
        if (channel != null) {
            connectionFactory.setChannel(channel);
        }
        connectionFactory.setQueueManager(queueManagerName);
        connectionFactory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);

        if (useSSL) {
            connectionFactory.setSSLCipherSuite("TLS_RSA_WITH_AES_128_CBC_SHA");

            // Denne trengs for at IBM MQ libs skal bruke/gjenkjenne samme ciphersuite navn
            // som Oracle JRE:
            // (Uten denne vil ikke IBM MQ libs gjenkjenne "TLS_RSA_WITH_AES_128_CBC_SHA")
            System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings", "false");
        }

        return connectionFactory;
    }
}
