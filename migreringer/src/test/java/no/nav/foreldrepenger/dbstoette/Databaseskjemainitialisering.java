package no.nav.foreldrepenger.dbstoette;

import java.util.List;

import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.util.env.Environment;

public final class Databaseskjemainitialisering {

    private static final Logger LOG = LoggerFactory.getLogger(Databaseskjemainitialisering.class);
    private static final Environment ENV = Environment.current();

    public static final List<DBProperties> DATASOURCES = List.of(cfg("defaultDS"), cfg("dvhDS"));

    public static void main(String[] args) {
        migrer();
    }

    public static void migrer() {
        DATASOURCES.forEach(p -> migrer(p));
    }

    public static void settJdniOppslag() {
        try {
            var props = defaultProperties();
            new EnvEntry("jdbc/" + props.getDatasource(), props.getDs());
        } catch (Exception e) {
            throw new RuntimeException("Feil under registrering av JDNI-entry for default datasource", e);
        }
    }

    public static DBProperties defaultProperties() {
        return DATASOURCES.stream()
                .filter(DBProperties::isDefaultDS)
                .findFirst()
                .orElseThrow();
    }

    private static void migrer(DBProperties props) {
        LOG.info("Migrerer {}", props);
        Flyway flyway = new Flyway();
        flyway.setBaselineOnMigrate(true);
        flyway.setDataSource(props.getDs());
        flyway.setTable("schema_version");
        flyway.setLocations(props.getScriptLocation());
        flyway.setCleanOnValidationError(true);
        if (!ENV.isLocal()) {
            throw new IllegalStateException("Forventer at denne migreringen bare kjøres lokalt");
        }
        flyway.migrate();
    }

    private static DBProperties cfg(String prefix) {
        String schema = ENV.getRequiredProperty(prefix + ".schema");
        return new DBProperties.Builder()
                .user(schema)
                .password(schema)
                .datasource(ENV.getRequiredProperty(prefix + ".datasource"))
                .schema(schema)
                .defaultDataSource(ENV.getProperty(prefix + ".default", boolean.class, false))
                .url(ENV.getRequiredProperty(prefix + ".url")).build();
    }
}
