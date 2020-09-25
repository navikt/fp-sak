package no.nav.foreldrepenger.dbstoette;

import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import no.nav.vedtak.util.env.Environment;

public final class Databaseskjemainitialisering {

    private static final Environment ENV = Environment.current();

    private static final List<DBProperties> UNIT_TEST = List.of(cfg("fpsak.default"), cfg("fpsak.hist"));

    private static final List<DBProperties> DBA = List.of(cfg("fpsak.dba"));

    private static final Logger LOG = LoggerFactory.getLogger(Databaseskjemainitialisering.class);

    public static void main(String[] args) {
        migrer();
    }

    public static void migrer() {
        try {
            kjørMigreringFor(DBA);
            kjørMigreringFor(UNIT_TEST);
        } catch (Exception e) {
            throw new RuntimeException("Feil under migrering av enhetstest-skjemaer", e);
        }
    }

    public static void settJdniOppslag() {
        try {
            var props = UNIT_TEST.stream()
                    .filter(DBProperties::isDefaultDataSource)
                    .findFirst()
                    .orElseThrow();
            new EnvEntry("jdbc/" + props.getDatasource(), ds(props));
        } catch (Exception e) {
            throw new RuntimeException("Feil under registrering av JDNI-entry for default datasource", e);
        }
    }

    private static void kjørMigreringFor(List<DBProperties> props) {
        props.forEach(Databaseskjemainitialisering::kjørerMigreringFor);
    }

    private static void kjørerMigreringFor(DBProperties props) {
        settOppDBSkjema(props);
    }

    private static void settOppDBSkjema(DBProperties props) {
        migrer(ds(props), props);
    }

    private static void migrer(DataSource ds, DBProperties props) {
        var cfg = new FlywayKonfig(ds);
        if (!cfg
                .medUsername(props.getUser())
                .medSqlLokasjon(scriptLocation(props))
                .medCleanup(props.isMigrateClean())
                .medMetadataTabell(props.getVersjonstabell())
                .migrerDb()) {
            LOG.warn(
                    "Kunne ikke starte inkrementell oppdatering av databasen. Det finnes trolig endringer i allerede kjørte script.\nKjører full migrering...");
            if (!cfg.medCleanup(true).migrerDb()) {
                throw new IllegalStateException("\n\nFeil i script. Avslutter...");
            }
        }
    }

    private static DBProperties cfg(String prefix) {
        String schema = ENV.getRequiredProperty(prefix + ".schema");
        return new DBProperties.Builder()
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

    private static String scriptLocation(DBProperties props) {
        return "classpath:/" + props.getMigrationScriptsClasspathRoot() + "/" + props.getSchema();
    }

    private static DataSource ds(DBProperties props) {
        var cfg = new HikariConfig();
        cfg.setJdbcUrl(props.getUrl());
        cfg.setUsername(props.getUser());
        cfg.setPassword(props.getPassword());

        cfg.setConnectionTimeout(1000);
        cfg.setMinimumIdle(0);
        cfg.setMaximumPoolSize(4);

        cfg.setAutoCommit(false);

        var dsProperties = new Properties();
        cfg.setDataSourceProperties(dsProperties);

        var ds = new HikariDataSource(cfg);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                ds.close();
            }
        }));
        return ds;
    }
}
