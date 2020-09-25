package no.nav.foreldrepenger.dbstoette;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import no.nav.vedtak.felles.lokal.dbstoette.DBConnectionProperties;
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

    private static final AtomicBoolean GUARD_UNIT_TEST_SKJEMAER = new AtomicBoolean();

    public static void main(String[] args) {
        migrerUnittestSkjemaer();
    }

    public static void migrerUnittestSkjemaer() {
        try {
            kjørMigreringFor(DBA);
            kjørMigreringFor(UNIT_TEST);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void settJdniOppslag() {
        try {
            var props = UNIT_TEST.stream()
                    .filter(DBConnectionProperties::isDefaultDataSource)
                    .findFirst()
                    .orElseThrow();
            new EnvEntry("jdbc/" + props.getDatasource(), ds(props));
        } catch (Exception e) {
            throw new RuntimeException("Feil under registrering av JDNI-entry for default datasource", e);
        }
    }

    private static void kjørMigreringFor(List<DBConnectionProperties> props) {
        props.forEach(Databaseskjemainitialisering::kjørerMigreringFor);
    }

    private static void kjørerMigreringFor(DBConnectionProperties props) {
        settOppDBSkjema(props);
    }

    private static void settOppDBSkjema(DBConnectionProperties props) {
        migrer(ds(props), props);
    }

    private static void migrer(DataSource ds, DBConnectionProperties props) {
        String scriptLocation = scriptLocation(props);

        boolean migreringOk = FlywayKonfig.lagKonfig(ds)
                .medSqlLokasjon(scriptLocation)
                .medCleanup(props.isMigrateClean(), props.getUser())
                .medMetadataTabell(props.getVersjonstabell())
                .migrerDb();

        if (!migreringOk) {
            LOG.warn(
                    "\n\nKunne ikke starte inkrementell oppdatering av databasen. Det finnes trolig endringer i allerede kjørte script.\nKjører full migrering...");

            migreringOk = FlywayKonfig.lagKonfig(ds)
                    .medCleanup(true, props.getUser())
                    .medSqlLokasjon(scriptLocation)
                    .medMetadataTabell(props.getVersjonstabell())
                    .migrerDb();
            if (!migreringOk) {
                throw new IllegalStateException("\n\nFeil i script. Avslutter...");
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

    private static String scriptLocation(DBConnectionProperties props) {
        return "classpath:/" + props.getMigrationScriptsClasspathRoot() + "/" + props.getSchema();
    }

    private static DataSource ds(DBConnectionProperties props) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getUrl());
        config.setUsername(props.getUser());
        config.setPassword(props.getPassword());

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
