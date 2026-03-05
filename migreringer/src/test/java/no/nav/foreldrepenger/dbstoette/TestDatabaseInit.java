package no.nav.foreldrepenger.dbstoette;

import static java.lang.Runtime.getRuntime;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import no.nav.foreldrepenger.konfig.Environment;

public final class TestDatabaseInit {

    public static final String DEFAULT_DS_SCHEMA = "defaultDS";

    public static final String TEST_DB_CONTAINER = Environment.current()
        .getProperty("testcontainer.test.db", String.class, "gvenzl/oracle-free:23-slim-faststart");
    public static final String DB_SETUP_SCRIPT_PATH =  getTestDbSetupScriptPath();

    private static final String INIT_SCRIPT_PATH = "../.oracle/oracle-init/fpsak.sql";

    // Per-schema migration guard to ensure migrations only run once per schema per JVM
    private static final ConcurrentHashMap<String, AtomicBoolean> MIGRATION_GUARDS = new ConcurrentHashMap<>();

    private static final Environment ENV = Environment.current();

    private static final String DB_SCRIPT_LOCATION = "/db/migration/";

    public static DataSource settOppDatasourceOgMigrer(String jdbcUrl, String username, String password, String schema) {
        var ds = createDatasource(jdbcUrl, username, password);
        // Use per-schema guard to support multiple datasources without re-running migrations
        var schemaGuard = MIGRATION_GUARDS.computeIfAbsent(schema, k -> new AtomicBoolean(false));
        if (schemaGuard.compareAndSet(false, true)) {
            var flyway = Flyway.configure()
                .dataSource(ds)
                .locations(getScriptLocation(schema))
                .baselineOnMigrate(true)
                .cleanDisabled(false)
                .load();
            try {
                if (!ENV.isLocal()) {
                    throw new IllegalStateException("Forventer at denne migreringen bare kjøres lokalt");
                }
                flyway.migrate();
            } catch (Exception ignore) {
                try {
                    flyway.clean();
                    flyway.migrate();
                } catch (FlywayException e) {
                    throw new RuntimeException("Flyway migrering feilet", e);
                }
            }
        }
        return ds;
    }

    private static String getScriptLocation(String schema) {
        return fileScriptLocation(schema);
    }

    private static String fileScriptLocation(String schema) {
        var relativePath = "migreringer/src/main/resources" + DB_SCRIPT_LOCATION + schema;
        var baseDir = new File(".").getAbsoluteFile();
        var location = new File(baseDir, relativePath);
        while (!location.exists()) {
            baseDir = baseDir.getParentFile();
            if (baseDir == null || !baseDir.isDirectory()) {
                throw new IllegalArgumentException("Klarte ikke finne : " + baseDir);
            }
            location = new File(baseDir, relativePath);
        }
        return "filesystem:" + location.getPath();
    }

    private static String getTestDbSetupScriptPath() {
        var initPath = INIT_SCRIPT_PATH;
        while (!(new File(initPath)).exists()) {
            initPath = "../" + initPath;
        }
        return initPath;
    }

    static void settJdniOppslag(DataSource dataSource) {
        try {
            new EnvEntry("jdbc/defaultDS", dataSource); // NOSONAR
        } catch (NamingException e) {
            throw new IllegalStateException("Feil under registrering av JDNI-entry for default datasource", e); // NOSONAR
        }
    }

    private static HikariDataSource createDatasource(String jdbcUrl, String username, String password) {
        var cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setUsername(username);
        cfg.setPassword(password);
        cfg.setConnectionTimeout(1500);
        cfg.setValidationTimeout(120L * 1000L);
        cfg.setMaximumPoolSize(4);
        cfg.setAutoCommit(false);
        var ds = new HikariDataSource(cfg);
        getRuntime().addShutdownHook(new Thread(ds::close));
        return ds;
    }
}
