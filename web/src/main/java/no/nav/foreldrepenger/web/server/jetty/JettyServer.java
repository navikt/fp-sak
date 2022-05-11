package no.nav.foreldrepenger.web.server.jetty;

import java.util.List;

import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.web.app.ApplicationConfig;
import no.nav.vedtak.isso.IssoApplication;

public class JettyServer extends AbstractJettyServer {

    private static final Logger LOG = LoggerFactory.getLogger(JettyServer.class);
    private DataSourceKonfig dataSourceKonfig;

    public JettyServer(int serverPort) {
        super(new JettyWebKonfigurasjon(serverPort));
    }

    public static void main(String[] args) throws Exception {
        jettyServer(args).bootStrap();
    }

    private static JettyServer jettyServer(String[] args) {
        if (args.length > 0) {
            return new JettyServer(Integer.parseUnsignedInt(args[0]));
        }
        return new JettyServer(8080);
    }

    protected void konfigurerMiljø() {
        dataSourceKonfig = new DataSourceKonfig();
    }

    @Override
    protected void konfigurerJndi() throws Exception {
        new EnvEntry("jdbc/defaultDS", dataSourceKonfig.defaultDS());
    }

    @Override
    protected void migrerDatabaser() {
        for (var cfg : dataSourceKonfig.getDataSources()) {
            LOG.info("Migrerer {}", cfg);
            flyway(cfg).migrate();
        }
    }

    public static Flyway flyway(DBConnProp cfg) {
        return Flyway.configure()
            .dataSource(cfg.getDatasource())
            .locations(cfg.getLocations())
            .baselineOnMigrate(true)
            .table("schema_version")
            .load();
    }

    @Override
    protected List<Class<?>> getWebInfClasses() {
        return List.of(ApplicationConfig.class, IssoApplication.class);
    }

}
