package no.nav.foreldrepenger.web.server.jetty;

import static no.nav.foreldrepenger.dbstoette.Databaseskjemainitialisering.migrer;
import static no.nav.foreldrepenger.dbstoette.Databaseskjemainitialisering.settJdniOppslag;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;

import no.nav.foreldrepenger.web.app.JettyTestApplication;
import no.nav.vedtak.util.env.Environment;

public class JettyDevServer extends JettyServer {

    private static final Environment ENV = Environment.current();
    private static final String TRUSTSTORE_PASSW_PROP = "javax.net.ssl.trustStorePassword";
    private static final String TRUSTSTORE_PATH_PROP = "javax.net.ssl.trustStore";

    public JettyDevServer() {
        super(new JettyDevKonfigurasjon());
    }

    public static void main(String[] args) throws Exception {
        new JettyDevServer().bootStrap();
    }

    @Override
    protected void konfigurer() throws Exception {
        System.setProperty("APP_LOG_HOME", "./logs");
        System.setProperty("conf", "src/main/resources/jetty/");
        super.konfigurer();
    }

    @Override
    protected void konfigurerJndi() throws Exception {
        settJdniOppslag();
        konfigurerJms();
    }

    @Override
    protected void migrerDatabaser() throws IOException {
        migrer();
    }

    @Override
    protected void konfigurerSikkerhet() {
        initCryptoStoreConfig("truststore", TRUSTSTORE_PATH_PROP, TRUSTSTORE_PASSW_PROP, "changeit");
        super.konfigurerSikkerhet();
    }

    @Override
    protected List<Connector> createConnectors(AppKonfigurasjon appKonfigurasjon, Server server) {
        var connectors = super.createConnectors(appKonfigurasjon, server);
        var https = createHttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());

        var sslConnector = new ServerConnector(server,
                new SslConnectionFactory(new SslContextFactory.Server(), "http/1.1"),
                new HttpConnectionFactory(https));
        sslConnector.setPort(appKonfigurasjon.getSslPort());
        connectors.add(sslConnector);
        return connectors;
    }

    @Override
    protected WebAppContext createContext(AppKonfigurasjon appKonfigurasjon) throws IOException {
        var ctx = super.createContext(appKonfigurasjon);
        ctx.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");
        return ctx;
    }

    @Override
    protected List<Class<?>> getWebInfClasses() {
        var classes = new ArrayList<>(super.getWebInfClasses());
        classes.add(JettyTestApplication.class);
        return classes;
    }

    @Override
    protected ResourceCollection createResourceCollection() throws IOException {
        return new ResourceCollection(
                Resource.newClassPathResource("META-INF/resources/webjars/"),
                Resource.newClassPathResource("/web"));
    }

    private static String initCryptoStoreConfig(String storeName, String storeProperty, String storePasswordProperty, String defaultPassword) {
        String defaultLocation = ENV.getProperty("user.home", ".") + "/.modig/" + storeName + ".jks";

        String storePath = ENV.getProperty(storeProperty, defaultLocation);
        File storeFile = new File(storePath);
        if (!storeFile.exists()) {
            throw new IllegalStateException("Finner ikke " + storeName + " i " + storePath
                    + "\n\tKonfigurer enten som System property \'" + storeProperty + "\' eller environment variabel \'"
                    + storeProperty.toUpperCase().replace('.', '_') + "\'");
        }
        String password = ENV.getProperty(storePasswordProperty, defaultPassword);
        if (password == null) {
            throw new IllegalStateException("Passord for å aksessere store " + storeName + " i " + storePath + " er null");
        }

        System.setProperty(storeProperty, storeFile.getAbsolutePath());
        System.setProperty(storePasswordProperty, password);
        return storePath;
    }
}
