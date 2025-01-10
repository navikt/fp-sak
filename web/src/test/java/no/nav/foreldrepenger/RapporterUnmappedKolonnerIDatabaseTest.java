package no.nav.foreldrepenger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.dbstoette.JpaExtension;

/**
 * Denne testen rapporterer kun tabeller og kolonner som ikke er mappet i
 * hibernate. Det kan være gyldige grunner til det (f.eks. dersom det kun
 * aksesseres gjennom native sql), men p.t. høyst sannsynlig ikke. Bør
 * gjennomgås jevnlig for å luke manglende contract av db skjema.
 */
@ExtendWith(JpaExtension.class)
class RapporterUnmappedKolonnerIDatabaseTest {
    private static final Logger LOG = LoggerFactory.getLogger(RapporterUnmappedKolonnerIDatabaseTest.class);

    private static final String HTE_PREFIX = "HTE_";
    private static final Set<String> UNNTA_TABELLER = Set.of("SCHEMA_VERSION", "STARTUPDATA");
    private static final Map<String, Set<String>> UNNTA_KOLONNER = Map.of(
        "BEHANDLING", Set.of("SIST_OPPDATERT_TIDSPUNKT"),
        "PROSESS_TASK", Set.of("SISTE_KJOERING_PLUKK_TS", "SISTE_KJOERING_SLUTT_TS")
    );

    private static EntityManagerFactory entityManagerFactory;

    public RapporterUnmappedKolonnerIDatabaseTest() {
    }

    @BeforeAll
    public static void setup() {

        Map<String, Object> configuration = new HashMap<>();

        configuration.put("hibernate.integrator_provider",
                (IntegratorProvider) () -> Collections.singletonList(
                        MetadataExtractorIntegrator.INSTANCE));

        entityManagerFactory = Persistence.createEntityManagerFactory("pu-default", configuration);
    }

    @AfterAll
    public static void teardown() {
        entityManagerFactory.close();
    }

    @SuppressWarnings("unchecked")
    private NavigableMap<String, Set<String>> getColumns(String namespace) {
        Predicate<Object[]> filterHte = (Object[] cols) -> !((String) cols[0]).toUpperCase().startsWith(HTE_PREFIX);
        Predicate<Object[]> filterSchemaVer = (Object[] cols) -> !UNNTA_TABELLER.contains(((String) cols[0]).toUpperCase());
        var groupingBy = Collectors.groupingBy((Object[] cols) -> ((String) cols[0]).toUpperCase(), TreeMap::new,
                Collectors.mapping((Object[] cols) -> ((String) cols[1]).toUpperCase(), Collectors.toCollection(TreeSet::new)));

        var em = entityManagerFactory.createEntityManager();
        try {
            if (namespace == null) {
                return (NavigableMap<String, Set<String>>) em
                        .createNativeQuery(
                                "select table_name, column_name from all_tab_cols where owner=SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND virtual_column='NO' AND hidden_column='NO'")
                        .getResultStream()
                        .filter(filterHte)
                        .filter(filterSchemaVer)
                        .collect(groupingBy);
            }
            return (NavigableMap<String, Set<String>>) em
                    .createNativeQuery(
                            "select table_name, column_name from all_tab_cols where owner=:ns AND virtual_column='NO' AND hidden_column='NO'")
                    .setParameter("ns", namespace)
                    .getResultStream()
                    .filter(filterHte)
                    .filter(filterSchemaVer)
                    .collect(groupingBy);
        } finally {
            em.close();
        }
    }

    @Test
    void sjekk_unmapped() { //NOSONAR greit å ikke ha asserts, skal bare logge
        sjekk_alle_tabeller_mappet();
        sjekk_alle_kolonner_mappet();
    }

    @SuppressWarnings("unchecked")
    private void sjekk_alle_kolonner_mappet() {
        for (var namespace : MetadataExtractorIntegrator.INSTANCE
                .getDatabase()
                .getNamespaces()) {
            var namespaceName = getSchemaName(namespace);
            var dbColumns = getColumns(namespaceName);
            for (var table : namespace.getTables()) {
                var columnNames = table.getColumns().stream().map(c -> c.getName().toUpperCase()).collect(Collectors.toCollection(TreeSet::new));
                var tableName = table.getName().toUpperCase();
                if (dbColumns.containsKey(tableName)) {
                    var unmapped = new TreeSet<>(dbColumns.get(tableName));
                    unmapped.removeAll(columnNames);
                    unmapped.removeAll(UNNTA_KOLONNER.getOrDefault(tableName, Set.of()));
                    if (!unmapped.isEmpty()) {
                        LOG.error("Table {} has unmapped columns: {}", table.getName(), unmapped);
                    }
                } else {
                    LOG.error("Table {} not in database schema {}", tableName, namespaceName);
                }
            }
        }

    }

    private void sjekk_alle_tabeller_mappet() {
        for (var namespace : MetadataExtractorIntegrator.INSTANCE
                .getDatabase()
                .getNamespaces()) {
            var namespaceName = getSchemaName(namespace);
            var dbColumns = getColumns(namespaceName);
            var dbTables = dbColumns.keySet();
            for (var table : namespace.getTables()) {
                var tableName = table.getName().toUpperCase();
                dbTables.remove(tableName);
            }
            dbTables.forEach(t -> LOG.error("Table not mapped in hibernate{}: {}", namespaceName, t));
        }

    }

    private String getSchemaName(Namespace namespace) {
        var schema = namespace.getName().getSchema();
        return schema == null ? null : schema.getCanonicalName().toUpperCase();
    }

    public static class MetadataExtractorIntegrator
            implements Integrator {

        public static final MetadataExtractorIntegrator INSTANCE = new MetadataExtractorIntegrator();

        private Database database;

        public Database getDatabase() {
            return database;
        }

        @Override
        public void integrate(
                Metadata metadata,
                SessionFactoryImplementor sessionFactory,
                SessionFactoryServiceRegistry serviceRegistry) {

            database = metadata.getDatabase();
        }

        @Override
        public void disintegrate(
                SessionFactoryImplementor sessionFactory,
                SessionFactoryServiceRegistry serviceRegistry) {
        }
    }

}
