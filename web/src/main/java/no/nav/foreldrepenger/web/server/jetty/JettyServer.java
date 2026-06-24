package no.nav.foreldrepenger.web.server.jetty;

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
import no.nav.vedtak.log.metrics.MetricsUtil;
import no.nav.vedtak.server.jetty.DataSourceShutdownListener;
import no.nav.vedtak.server.jetty.JettyServerBuilder;

public class JettyServer {

    private static final Environment ENV = Environment.current();

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
        MetricsUtil.init(); // Sett opp registry før andre kobler seg på
        konfigurerLogging();
        konfigurerSystembruker();

        migrerDatabase(NamingStandard.DEFAULT_DATA_SOURCE);
        migrerDatabase("dvhDS");

        // Sett System.setProperty("task.manager.runner.threads", 10); til å endre antal prosesstask tråder. Default 10.
        // `maxPoolSize` bør være satt minst til verdien av `task.manager.runner.threads` + 1 + antall connections man ønsker.
        createDatasource();

        start();
    }

    /**
     * Vi bruker SLF4J + logback, Jersey brukes JUL for logging.
     * Setter opp en bridge til å få Jersey til å logge gjennom Logback også.
     */
    private void konfigurerLogging() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
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

    static void createDatasource() {
        var jdbc = hentEllerBeregnVerdiHvisMangler(NamingStandard.DEFAULT_DATA_SOURCE + ".url", NamingStandard.DEFAULT_DATA_SOURCE + "config", "jdbc_url");
        var username = hentEllerBeregnVerdiHvisMangler(NamingStandard.DEFAULT_DATA_SOURCE + ".username", NamingStandard.DEFAULT_DATA_SOURCE, "username");
        var password = hentEllerBeregnVerdiHvisMangler(NamingStandard.DEFAULT_DATA_SOURCE + ".password", NamingStandard.DEFAULT_DATA_SOURCE, "password");
        var dataSource = DatasourceUtil.oracleDataSource(jdbc, username, password, 30);
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
        var server = JettyServerBuilder.builder()
            .port(getServerPort())
            .contextPath(CONTEXT_PATH)
            .withForwardedRequestCustomizer()
            .addEventListener(new ServiceStarterListener())
            .addEventListener(new DataSourceShutdownListener(DataSourceHolder::close))
            .registerRestApp(InternalApiConfig.API_URI, InternalApiConfig.class)
            .registerRestApp(ApiConfig.API_URI, ApiConfig.class)
            .registerRestApp(ForvaltningApiConfig.API_URI, ForvaltningApiConfig.class)
            .registerRestApp(EksternApiConfig.API_URI, EksternApiConfig.class)
            .build();
        server.start();
        server.join();
    }

    private Integer getServerPort() {
        return this.serverPort;
    }

}
