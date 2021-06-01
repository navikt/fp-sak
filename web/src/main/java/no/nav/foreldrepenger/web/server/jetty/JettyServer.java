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
        tull();
    }

    private void tull() {

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
    }

    @Override
    protected void konfigurerJndi() throws Exception {
        new EnvEntry("jdbc/defaultDS", dataSourceKonfig.defaultDS());
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
    protected List<Class<?>> getWebInfClasses() {
        return List.of(ApplicationConfig.class, IssoApplication.class);
    }

}
