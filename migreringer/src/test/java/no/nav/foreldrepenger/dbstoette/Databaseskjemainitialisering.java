package no.nav.foreldrepenger.dbstoette;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import no.nav.vedtak.felles.lokal.dbstoette.DBConnectionProperties;
import no.nav.vedtak.felles.lokal.dbstoette.DatabaseStøtte;
import no.nav.vedtak.felles.testutilities.db.FlywayKonfig;
import no.nav.vedtak.util.env.Environment;

/**
 * Initielt skjemaoppsett + migrering av unittest-skjemaer
 */
public final class Databaseskjemainitialisering {

    private static final Environment ENV = Environment.current();

    private static final List<DBConnectionProperties> UNIT_TEST = List.of(cfg("fpsak.default"), cfg("fpsak.hist"));

    private static final List<DBConnectionProperties> DBA = List.of(cfg("fpsak.dba"));

    private static final Logger LOG = LoggerFactory.getLogger(Databaseskjemainitialisering.class);

    private static final AtomicBoolean GUARD_SKJEMAER = new AtomicBoolean();
    private static final AtomicBoolean GUARD_UNIT_TEST_SKJEMAER = new AtomicBoolean();

    public static void main(String[] args) {
        migrerUnittestSkjemaer();
    }

    public static void migrerUnittestSkjemaer() {
        settOppSkjemaer();

        if (GUARD_UNIT_TEST_SKJEMAER.compareAndSet(false, true)) {
            try {
                DatabaseStøtte.kjørMigreringFor(UNIT_TEST);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void settOppSkjemaer() {
        if (GUARD_SKJEMAER.compareAndSet(false, true)) {
            try {
                DatabaseStøtte.kjørMigreringFor(DBA);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static DBConnectionProperties cfg(String prefix) {
        String schema = ENV.getRequiredProperty(prefix + ".schema");
        return new DBConnectionProperties.Builder()
                .user(schema)
                .versjonstabell("schema_version")
                .password(schema)
                .datasource(ENV.getRequiredProperty(prefix + ".datasource"))
                .schema(schema)
                .defaultSchema(ENV.getProperty(prefix + ".defaultschema", schema))
                .defaultDataSource(ENV.getProperty(prefix + ".default", boolean.class, false))
                .migrateClean(ENV.getProperty(prefix + ".migrateclean", boolean.class, true))
                .url(ENV.getRequiredProperty(prefix + ".url"))
                .migrationScriptsFilesystemRoot(ENV.getRequiredProperty(prefix + ".ms")).build();
    }

    public static void settJdniOppslag() {
        try {
            settOppJndiForDefaultDataSource(UNIT_TEST);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void settOppJndiForDefaultDataSource(List<DBConnectionProperties> allDbConnectionProperties) {
        Optional<DBConnectionProperties> defaultDataSource = DBConnectionProperties.finnDefault(allDbConnectionProperties);
        defaultDataSource.ifPresent(Databaseskjemainitialisering::settOppJndiDataSource);
    }

    private static void settOppJndiDataSource(DBConnectionProperties defaultConnectionProperties) {
        try {
            new EnvEntry("jdbc/" + defaultConnectionProperties.getDatasource(), ds(defaultConnectionProperties));
        } catch (NamingException e) {
            throw new RuntimeException("Feil under registrering av JDNI-entry for default datasource", e); // NOSONAR
        }
    }

    public static void kjørMigreringFor(List<DBConnectionProperties> connectionProperties) {
        connectionProperties.forEach(Databaseskjemainitialisering::kjørerMigreringFor);
    }

    private static void kjørerMigreringFor(DBConnectionProperties connectionProperties) {
        settOppDBSkjema(connectionProperties);
    }

    private static void settOppDBSkjema(DBConnectionProperties dbProperties) {
        migrer(ds(dbProperties), dbProperties);

    }

    private static void migrer(DataSource dataSource,
            DBConnectionProperties connectionProperties) {
        String scriptLocation;
        if (connectionProperties.getMigrationScriptsClasspathRoot() != null) {
            scriptLocation = "classpath:/" + connectionProperties.getMigrationScriptsClasspathRoot() + "/"
                    + connectionProperties.getSchema();
        } else {
            scriptLocation = getMigrationScriptLocation(connectionProperties);
        }

        boolean migreringOk = FlywayKonfig.lagKonfig(dataSource)
                .medSqlLokasjon(scriptLocation)
                .medCleanup(connectionProperties.isMigrateClean(), connectionProperties.getUser())
                .medMetadataTabell(connectionProperties.getVersjonstabell())
                .migrerDb();

        if (!migreringOk) {
            LOG.warn(
                    "\n\nKunne ikke starte inkrementell oppdatering av databasen. Det finnes trolig endringer i allerede kjørte script.\nKjører full migrering...");

            migreringOk = FlywayKonfig.lagKonfig(dataSource)
                    .medCleanup(true, connectionProperties.getUser())
                    .medSqlLokasjon(scriptLocation)
                    .medMetadataTabell(connectionProperties.getVersjonstabell())
                    .migrerDb();
            if (!migreringOk) {
                throw new IllegalStateException("\n\nFeil i script. Avslutter...");
            }
        }
    }

    private static String getMigrationScriptLocation(DBConnectionProperties connectionProperties) {
        String relativePath = connectionProperties.getMigrationScriptsFilesystemRoot() + connectionProperties.getDatasource();
        File baseDir = new File(".").getAbsoluteFile();
        File location = new File(baseDir, relativePath);
        while (!location.exists()) {
            baseDir = baseDir.getParentFile();
            if (baseDir == null || !baseDir.isDirectory()) {
                throw new IllegalArgumentException("Klarte ikke finne : " + baseDir);
            }
            location = new File(baseDir, relativePath);
        }

        return "filesystem:" + location.getPath();
    }

    private static DataSource ds(DBConnectionProperties dbProperties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbProperties.getUrl());
        config.setUsername(dbProperties.getUser());
        config.setPassword(dbProperties.getPassword());

        config.setConnectionTimeout(1000);
        config.setMinimumIdle(0);
        config.setMaximumPoolSize(4);

        config.setAutoCommit(false);

        Properties dsProperties = new Properties();
        config.setDataSourceProperties(dsProperties);

        HikariDataSource ds = new HikariDataSource(config);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                ds.close();
            }
        }));

        return ds;
    }
}
