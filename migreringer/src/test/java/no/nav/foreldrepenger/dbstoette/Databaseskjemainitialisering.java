package no.nav.foreldrepenger.dbstoette;

import static java.lang.Runtime.getRuntime;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import no.nav.foreldrepenger.konfig.Environment;

public final class Databaseskjemainitialisering {

    private static final AtomicBoolean GUARD_UNIT_TEST_SKJEMAER = new AtomicBoolean();

    private static final Logger LOG = LoggerFactory.getLogger(Databaseskjemainitialisering.class);
    private static final Environment ENV = Environment.current();

    private static final String DB_SCRIPT_LOCATION = "/db/migration/";

    private static final DataSource DEFAULT_DS = settJdniOppslag();
    private static final String DEFAULTDS_SCHEMA = "defaultDS";
    public static final String DEFAULTDS_USER = "fpsak";

    public static void main(String[] args) {
        //brukes i mvn clean install
        migrerUnittestSkjemaer();
    }

    public static DataSource initUnitTestDataSource() {
        if (DEFAULT_DS != null) {
            return DEFAULT_DS;
        }
        settJdniOppslag();
        return DEFAULT_DS;
    }

    public static void migrerUnittestSkjemaer() {
        migrerUnittestSkjemaer(DEFAULTDS_SCHEMA, DEFAULTDS_USER);
        migrerUnittestSkjemaer("dvhDS", "fpsak_hist");
    }

    private static void migrerUnittestSkjemaer(String schemaName, String user) {
        if (GUARD_UNIT_TEST_SKJEMAER.compareAndSet(false, true)) {
            var flyway = Flyway.configure()
                .dataSource(createDs(user))
                .locations(DB_SCRIPT_LOCATION + schemaName)
                .table("schema_version")
                .baselineOnMigrate(true)
                .cleanOnValidationError(true)
                .load();
            try {
                if (!ENV.isLocal()) {
                    throw new IllegalStateException("Forventer at denne migreringen bare kjøres lokalt");
                }
                flyway.migrate();
            } catch (FlywayException fwe) {
                try {
                    // prøver igjen
                    flyway.clean();
                    flyway.migrate();
                } catch (FlywayException fwe2) {
                    throw new IllegalStateException("Migrering feiler", fwe2);
                }
            }
        }
    }

    public static synchronized DataSource settJdniOppslag() {
        var ds = createDs(DEFAULTDS_USER);
        try {
            new EnvEntry("jdbc/defaultDS", ds); // NOSONAR
            return ds;
        } catch (NamingException e) {
            throw new IllegalStateException("Feil under registrering av JDNI-entry for defaultDS", e); // NOSONAR
        }
    }

    private static DataSource createDs(String user) {
        var cfg = new HikariConfig();
        cfg.setJdbcUrl(buildJdbcUrl());
        cfg.setUsername(ENV.getProperty("database.user", user));
        cfg.setPassword(ENV.getProperty("database.password", user));
        cfg.setConnectionTimeout(1500);
        cfg.setValidationTimeout(120L * 1000L);
        cfg.setMaximumPoolSize(4);
        cfg.setAutoCommit(false);
        var ds = new HikariDataSource(cfg);
        getRuntime().addShutdownHook(new Thread(ds::close));
        return ds;
    }

    private static String buildJdbcUrl() {
        return String.format("jdbc:oracle:thin:@//%s:%s/%s",
            ENV.getProperty("database.host", "localhost"),
            ENV.getProperty("database.post", "1521"),
            ENV.getProperty("database.service", "XE"));
    }
}
