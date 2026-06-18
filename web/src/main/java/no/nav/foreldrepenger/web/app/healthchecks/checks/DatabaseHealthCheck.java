package no.nav.foreldrepenger.web.app.healthchecks.checks;

import java.sql.SQLException;

import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.jpa.jdbc.DataSourceHolder;
import no.nav.vedtak.server.LiveAndReadinessAware;


@ApplicationScoped
public class DatabaseHealthCheck implements LiveAndReadinessAware {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseHealthCheck.class);
    private static final String SQL_QUERY = "select 1 from DUAL";

    DatabaseHealthCheck() {
        // CDI
    }

    private boolean isOK() {
        if (!DataSourceHolder.isInitialized()) {
            return false;
        }
        try (var connection = DataSourceHolder.getDataSource().getConnection()) {
            try (var statement = connection.createStatement()) {
                if (!statement.execute(SQL_QUERY)) {
                    LOG.warn("Feil ved SQL-spørring {} mot databasen", SQL_QUERY);
                    return false;
                }
            }
        } catch (SQLException e) {
            LOG.warn("Feil ved SQL-spørring {} mot databasen", SQL_QUERY);
            return false;
        }

        return true;
    }

    @Override
    public boolean isReady() {
        return isOK();
    }

    @Override
    public boolean isAlive() {
        return isOK();
    }
}
