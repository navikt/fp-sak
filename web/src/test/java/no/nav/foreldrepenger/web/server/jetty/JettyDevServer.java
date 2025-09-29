package no.nav.foreldrepenger.web.server.jetty;

import no.nav.foreldrepenger.konfig.Environment;


public class JettyDevServer extends JettyServer {

    private static final Environment ENV = Environment.current();

    public static void main(String[] args) throws Exception {
        // Konfigurerer tasker til å polle mer aggressivt, gjør at verdikjede kjører raskere lokalt
        System.setProperty("task.manager.polling.delay", "40");
        System.setProperty("task.manager.runner.threads", "4");
        initTrustStoreAndKeyStore();
        jettyServer(args).bootStrap();
    }

    protected static JettyDevServer jettyServer(String[] args) {
        if (args.length > 0) {
            return new JettyDevServer(Integer.parseUnsignedInt(args[0]));
        }
        return new JettyDevServer(ENV.getProperty("server.port", Integer.class, 8080));
    }

    private JettyDevServer(int serverPort) {
        super(serverPort);
    }

    private static void initTrustStoreAndKeyStore() {
        var keystoreRelativPath = ENV.getProperty("KEYSTORE_RELATIV_PATH");
        var truststoreRelativPath = ENV.getProperty("TRUSTSTORE_RELATIV_PATH");
        var keystoreTruststorePassword = ENV.getProperty("JAVAX_NET_SSL_PASSWORD");
        var absolutePathHome = ENV.getProperty("user.home", ".");
        System.setProperty("javax.net.ssl.trustStore", absolutePathHome + truststoreRelativPath);
        System.setProperty("javax.net.ssl.keyStore", absolutePathHome + keystoreRelativPath);
        System.setProperty("javax.net.ssl.trustStorePassword", keystoreTruststorePassword);
        System.setProperty("javax.net.ssl.keyStorePassword", keystoreTruststorePassword);
        System.setProperty("javax.net.ssl.password", keystoreTruststorePassword);
        // KAFKA spesifikke properties
        System.setProperty("KAFKA_TRUSTSTORE_PATH", absolutePathHome + truststoreRelativPath);
        System.setProperty("KAFKA_KEYSTORE_PATH", absolutePathHome + keystoreRelativPath);
        System.setProperty("KAFKA_CREDSTORE_PASSWORD", keystoreTruststorePassword);
    }
}
