package no.nav.foreldrepenger.web.server.jetty;

import java.util.ArrayList;
import java.util.List;

import no.nav.vedtak.log.metrics.MetricsUtil;

import org.eclipse.jetty.ee11.cdi.CdiDecoratingListener;
import org.eclipse.jetty.ee11.cdi.CdiServletContainerInitializer;
import org.eclipse.jetty.ee11.servlet.DefaultServlet;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.servlet.security.ConstraintMapping;
import org.eclipse.jetty.ee11.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.web.app.konfig.ApiConfig;
import no.nav.foreldrepenger.web.app.konfig.EksternApiConfig;
import no.nav.foreldrepenger.web.app.konfig.ForvaltningApiConfig;
import no.nav.foreldrepenger.web.app.konfig.InternalApiConfig;
import no.nav.foreldrepenger.web.app.tjenester.ServiceStarterListener;
import no.nav.vedtak.felles.jpa.NamingStandard;
import no.nav.vedtak.felles.jpa.flyway.FlywayUtil;
import no.nav.vedtak.felles.jpa.jdbc.DataSourceHolder;
import no.nav.vedtak.felles.jpa.jdbc.DatasourceUtil;

public class JettyServer {

    private static final Environment ENV = Environment.current();
    private static final Logger LOG = LoggerFactory.getLogger(JettyServer.class);
    private static final String APPLICATION = "jakarta.ws.rs.Application";

    private static final String CONTEXT_PATH = ENV.getProperty("context.path", "/fpsak");

    private final Integer serverPort;

    static void main() throws Exception {
        jettyServer().bootStrap();
    }

    protected static JettyServer jettyServer() {
        return new JettyServer(ENV.getProperty("server.port", Integer.class, 8080));
    }

    protected JettyServer(int serverPort) {
        this.serverPort = serverPort;
    }

    protected void bootStrap() throws Exception {
        konfigurerLogging();
        konfigurerSystembruker();

        migrerDatabase(NamingStandard.DEFAULT_DATA_SOURCE);
        migrerDatabase("dvhDS");

        // Sett System.setProperty("task.manager.runner.threads", 10); til å endre antal prosesstask tråder. Default 10.
        // `maxPoolSize` bør være satt minst til verdien av `task.manager.runner.threads` + 1 + antall connections man ønsker.
        createDatasource(NamingStandard.DEFAULT_DATA_SOURCE, 30);

        start();
    }

    /**
     * Vi bruker SLF4J + logback, Jersey brukes JUL for logging.
     * Setter opp en bridge til å få Jersey til å logge gjennom Logback også.
     */
    private void konfigurerLogging() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        MetricsUtil.scrape(); // TODO: erstatt med kommende init
    }

    /* Brukes kun for å kunne samhandle med Økonomi via JMS */
    private static void konfigurerSystembruker() {
        settSystembrukerVerdiHvisMangler("systembruker.username", "username");
        settSystembrukerVerdiHvisMangler("systembruker.password", "password");
    }

    private static void settSystembrukerVerdiHvisMangler(String key, String filNavn) {
        if (ENV.getProperty(key) == null) {
            System.getProperties().computeIfAbsent(key, _ -> VaultUtil.lesFilVerdi("serviceuser", filNavn));
        }
    }

    protected void migrerDatabase(String schemaName) {
        var jdbc = hentEllerBeregnVerdiHvisMangler(schemaName + ".url",schemaName + "config", "jdbc_url");
        var username = hentEllerBeregnVerdiHvisMangler(schemaName + ".username", schemaName, "username");
        var password = hentEllerBeregnVerdiHvisMangler(schemaName + ".password", schemaName, "password");
        try (var dataSource = FlywayUtil.createMigrationDataSource(jdbc, username, password)) {
            FlywayUtil.migrateLegacyOracle(dataSource, "classpath:" + NamingStandard.DEFAULT_MIGRATION_ROOT + schemaName);
        }
    }

    static void createDatasource(String schemaName, int maxPoolSize) {
        var jdbc = hentEllerBeregnVerdiHvisMangler(schemaName + ".url",schemaName + "config", "jdbc_url");
        var username = hentEllerBeregnVerdiHvisMangler(schemaName + ".username", schemaName, "username");
        var password = hentEllerBeregnVerdiHvisMangler(schemaName + ".password", schemaName, "password");
        var dataSource = DatasourceUtil.oracleDataSource(jdbc, username, password, maxPoolSize);
        DataSourceHolder.initialize(dataSource);
    }

    /* Denne gir lazy loading og feiler ikke ved lokalt kjøring uten vault mount */
    private static String hentEllerBeregnVerdiHvisMangler(String key, String mappeNavn, String filNavn) {
        if (ENV.getProperty(key) == null) {
            System.getProperties().computeIfAbsent(key, _ -> VaultUtil.lesFilVerdi(mappeNavn, filNavn));
        }
        return ENV.getRequiredProperty(key);
    }

    private void start() throws Exception {
        var server = new Server(getServerPort());
        server.setConnectors(createConnectors(server).toArray(new Connector[]{}));
        server.setHandler(createContext());
        server.start();
        server.join();
    }

    private List<Connector> createConnectors(Server server) {
        List<Connector> connectors = new ArrayList<>();
        var httpConnector = new ServerConnector(server, new HttpConnectionFactory(createHttpConfiguration()));
        httpConnector.setPort(getServerPort());
        connectors.add(httpConnector);
        return connectors;
    }

    private static HttpConfiguration createHttpConfiguration() {
        var httpConfig = new HttpConfiguration();
        // Add support for X-Forwarded headers
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());
        return httpConfig;
    }

    private static ContextHandler createContext() {
        var ctx = new ServletContextHandler(CONTEXT_PATH, ServletContextHandler.NO_SESSIONS);

        // Sikkerhet
        ctx.setSecurityHandler(simpleConstraints());

        // Servlets
        registerDefaultServlet(ctx);
        registerServlet(ctx, 0, InternalApiConfig.API_URI, InternalApiConfig.class);
        registerServlet(ctx, 1, ApiConfig.API_URI, ApiConfig.class);
        registerServlet(ctx, 2, ForvaltningApiConfig.API_URI, ForvaltningApiConfig.class);
        registerServlet(ctx, 3, EksternApiConfig.API_URI, EksternApiConfig.class);

        // Starter tjenester
        ctx.addEventListener(new ServiceStarterListener());

        // Enable Weld + CDI
        ctx.setInitParameter(CdiServletContainerInitializer.CDI_INTEGRATION_ATTRIBUTE, CdiDecoratingListener.MODE);
        ctx.addServletContainerInitializer(new CdiServletContainerInitializer());
        ctx.addServletContainerInitializer(new org.jboss.weld.environment.servlet.EnhancedListener());

        return ctx;
    }

    private static void registerDefaultServlet(ServletContextHandler context) {
        var defaultServlet = new ServletHolder(new DefaultServlet());
        context.addServlet(defaultServlet, "/*");
    }

    private static void registerServlet(ServletContextHandler context, int prioritet, String path, Class<?> appClass) {
        var servlet = new ServletHolder(new ServletContainer());
        servlet.setName(appClass.getName());
        servlet.setInitOrder(prioritet);
        servlet.setInitParameter(APPLICATION, appClass.getName());
        context.addServlet(servlet, path + "/*");
    }

    private static ConstraintSecurityHandler simpleConstraints() {
        var handler = new ConstraintSecurityHandler();
        // Slipp gjennom kall fra plattform til JaxRs. Foreløpig kun behov for GET
        handler.addConstraintMapping(pathConstraint(Constraint.ALLOWED, InternalApiConfig.API_URI + "/*"));
        // Slipp gjennom til autentisering i JaxRs / auth-filter
        handler.addConstraintMapping(pathConstraint(Constraint.ALLOWED, ApiConfig.API_URI + "/*"));
        // Slipp gjennom til autentisering i JaxRs / auth-filter
        handler.addConstraintMapping(pathConstraint(Constraint.ALLOWED, ForvaltningApiConfig.API_URI + "/*"));
        // Slipp gjennom til autentisering i JaxRs / auth-filter
        handler.addConstraintMapping(pathConstraint(Constraint.ALLOWED, EksternApiConfig.API_URI + "/*"));
        // Alt annet av paths og metoder forbudt - 403
        handler.addConstraintMapping(pathConstraint(Constraint.FORBIDDEN, "/*"));
        return handler;
    }

    private static ConstraintMapping pathConstraint(Constraint constraint, String path) {
        var mapping = new ConstraintMapping();
        mapping.setConstraint(constraint);
        mapping.setPathSpec(path);
        return mapping;
    }

    private Integer getServerPort() {
        return this.serverPort;
    }

}
