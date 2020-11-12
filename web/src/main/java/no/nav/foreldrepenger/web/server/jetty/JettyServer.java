package no.nav.foreldrepenger.web.server.jetty;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.jms.JMSException;
import javax.naming.NamingException;

import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.webapp.MetaData;
import org.eclipse.jetty.webapp.WebAppContext;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.web.app.ApplicationConfig;
import no.nav.vedtak.isso.IssoApplication;

public class JettyServer extends AbstractJettyServer {

    private static final Logger LOG = LoggerFactory.getLogger(JettyServer.class);
    private DataSourceKonfig dataSourceKonfig;

    public JettyServer() {
        this(new JettyWebKonfigurasjon());
    }

    public JettyServer(int serverPort) {
        this(new JettyWebKonfigurasjon(serverPort));
    }

    JettyServer(AppKonfigurasjon appKonfigurasjon) {
        super(appKonfigurasjon);
    }

    public static void main(String[] args) throws Exception {
        jettyServer(args).bootStrap();
    }

    private static JettyServer jettyServer(String[] args) {
        if (args.length > 0) {
            return new JettyServer(Integer.parseUnsignedInt(args[0]));
        }
        return new JettyServer();
    }

    @Override
    protected void konfigurerMiljø() {
        dataSourceKonfig = new DataSourceKonfig();
        tull();
    }

    private void tull() {

        if (System.getenv("LOADBALANCER_FQDN") != null) {
            LOG.info("Trickser med loadbalanser.url");
            String loadbalancerFqdn = System.getenv("LOADBALANCER_FQDN");
            String protocol = (loadbalancerFqdn.startsWith("localhost")) ? "http" : "https";
            System.setProperty("loadbalancer.url", protocol + "://" + loadbalancerFqdn);
        }
        // FIXME (u139158): PFP-1176 Skriv om i OpenAmIssoHealthCheck og
        // AuthorizationRequestBuilder når Jboss dør
        if (System.getenv("OIDC_OPENAM_HOSTURL") != null) {
            LOG.info("Trickser med OIDC_OPENAM_HOSTURL");
            System.setProperty("OpenIdConnect.issoHost", System.getenv("OIDC_OPENAM_HOSTURL"));
        }
        // FIXME (u139158): PFP-1176 Skriv om i AuthorizationRequestBuilder og
        // IdTokenAndRefreshTokenProvider når Jboss dør
        if (System.getenv("OIDC_OPENAM_AGENTNAME") != null) {
            LOG.info("Trickser med OIDC_OPENAM_AGENTNAME");
            System.setProperty("OpenIdConnect.username", System.getenv("OIDC_OPENAM_AGENTNAME"));
        }
        // FIXME (u139158): PFP-1176 Skriv om i IdTokenAndRefreshTokenProvider når Jboss
        // dør
        if (System.getenv("OIDC_OPENAM_PASSWORD") != null) {
            LOG.info("Trickser med OIDC_OPENAM_PASSWORD");
            System.setProperty("OpenIdConnect.password", System.getenv("OIDC_OPENAM_PASSWORD"));
        }
        // FIXME (u139158): PFP-1176 Skriv om i BaseJmsKonfig når Jboss dør
        if (System.getenv("FPSAK_CHANNEL_NAME") != null) {
            LOG.info("Trickser med OIDC_OPENAM_PASSWORD");
            System.setProperty("mqGateway02.channel", System.getenv("FPSAK_CHANNEL_NAME"));
        }
    }

    @Override
    protected void konfigurerJndi() throws Exception {
        new EnvEntry("jdbc/defaultDS", dataSourceKonfig.defaultDS());
        konfigurerJms();
    }

    protected void konfigurerJms() throws JMSException, NamingException {
        JmsKonfig.settOppJndiConnectionfactory("jms/ConnectionFactory", "mqGateway02", "fpsak.channel");
        JmsKonfig.settOppJndiMessageQueue("jms/QueueFpsakOkonomiOppdragSend", "fpsak.okonomi.oppdrag.send");
        JmsKonfig.settOppJndiMessageQueue("jms/QueueFpsakOkonomiOppdragMotta", "fpsak.okonomi.oppdrag.mottak");
        JmsKonfig.settOppJndiMessageQueue("jms/QueueFpsakGrensesnittavstemmingSend", "ray.avstem.data", true);
    }

    @Override
    protected void migrerDatabaser() {
        for (var cfg : dataSourceKonfig.getDataSources()) {
            LOG.info("Migrerer {}", cfg);
            var flyway = new Flyway();
            flyway.setDataSource(cfg.getDatasource());
            flyway.setLocations(cfg.getLocations());
            flyway.setBaselineOnMigrate(true);
            flyway.migrate();
        }
    }

    @Override
    protected WebAppContext createContext(AppKonfigurasjon appKonfigurasjon) throws IOException {
        var webAppContext = super.createContext(appKonfigurasjon);
        webAppContext.setParentLoaderPriority(true);
        updateMetaData(webAppContext.getMetaData());
        return webAppContext;
    }

    private void updateMetaData(MetaData metaData) {
        // Find path to class-files while starting jetty from development environment.
        var resources = getWebInfClasses().stream()
                .map(c -> Resource.newResource(c.getProtectionDomain().getCodeSource().getLocation()))
                .distinct()
                .collect(Collectors.toList());

        metaData.setWebInfClassesDirs(resources);
    }

    protected List<Class<?>> getWebInfClasses() {
        return List.of(ApplicationConfig.class, IssoApplication.class);
    }

    @Override
    protected ResourceCollection createResourceCollection() throws IOException {
        return new ResourceCollection(
                Resource.newClassPathResource("META-INF/resources/webjars/"),
                Resource.newClassPathResource("/web"));
    }

}
