package no.nav.foreldrepenger.web.app.healthchecks.checks;

import static no.nav.foreldrepenger.web.server.jetty.DataSourceKonfig.DEFAULT_DS_NAME;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;

import javax.enterprise.context.ApplicationScoped;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.flywaydb.core.api.MigrationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.web.server.jetty.DBConnProp;
import no.nav.foreldrepenger.web.server.jetty.JettyServer;

@ApplicationScoped
public class DatabaseHealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseHealthCheck.class);
    private static final String JNDI_NAME = "jdbc/" + DEFAULT_DS_NAME;

    private String jndiName;

    private static final String SQL_QUERY = "select 1 from dual";
    // må være rask, og bruke et stabilt tabell-navn

    private String endpoint = null; // ukjent frem til første gangs test

    public DatabaseHealthCheck() {
        this.jndiName = JNDI_NAME;
    }

    public String getDescription() {
        return "Test av databaseforbindelse (" + jndiName + ")";
    }

    public String getEndpoint() {
        return endpoint;
    }

    public boolean isOK() {

        DataSource dataSource;
        try {
            dataSource = (DataSource) new InitialContext().lookup(jndiName);
        } catch (NamingException e) {
            return false;
        }

        try (var connection = dataSource.getConnection()) {
            if (endpoint == null) {
                endpoint = extractEndpoint(connection);
            }
            try (var statement = connection.createStatement()) {
                if (!statement.execute(SQL_QUERY)) {
                    throw new SQLException("SQL-spørring ga ikke et resultatsett");
                }
            }
            var flyway = JettyServer.flyway(new DBConnProp(dataSource, DEFAULT_DS_NAME));
            var flywaySuccess = Arrays.stream(flyway.info().all())
                .allMatch(migrationInfo -> migrationInfo.getState().equals(MigrationState.SUCCESS));
            if (!flywaySuccess) {
                return false;
            }
        } catch (SQLException e) {
            LOG.warn("Feil ved SQL-spørring {} mot databasen", SQL_QUERY);
            return false;
        }

        return true;
    }

    private String extractEndpoint(Connection connection) {
        var result = "?";
        try {
            var metaData = connection.getMetaData();
            var url = metaData.getURL();
            if (url != null) {
                if (!url.toUpperCase(Locale.US).contains("SERVICE_NAME=")) { // don't care about Norwegian letters here
                    url = url + "/" + connection.getSchema();
                }
                result = url;
            }
        } catch (SQLException e) { //NOSONAR
            // ikke fatalt
        }
        return result;
    }
}
