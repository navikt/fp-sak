package no.nav.foreldrepenger.web.app.healthchecks.checks;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import javax.enterprise.context.ApplicationScoped;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DatabaseHealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseHealthCheck.class);
    private static final String JDBC_DEFAULT_DS = "jdbc/defaultDS";

    private String jndiName;

    private static final String SQL_QUERY = "select 1 from dual";
    // må være rask, og bruke et stabilt tabell-navn

    private String endpoint = null; // ukjent frem til første gangs test

    public DatabaseHealthCheck() {
        this.jndiName = JDBC_DEFAULT_DS;
    }

    public String getDescription() {
        return "Test av databaseforbindelse (" + jndiName + ")";
    }

    public String getEndpoint() {
        return endpoint;
    }

    public boolean isOK() {

        DataSource dataSource = null;
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
