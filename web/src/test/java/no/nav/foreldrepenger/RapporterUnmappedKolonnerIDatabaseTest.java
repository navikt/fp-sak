package no.nav.foreldrepenger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.mapping.Column;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.dbstoette.Databaseskjemainitialisering;

/**
 * Denne testen rapporterer kun tabeller og kolonner som ikke er mappet i
 * hibernate. Det kan være gyldige grunner til det (f.eks. dersom det kun
 * aksesseres gjennom native sql), men p.t. høyst sannsynlig ikke. Bør
 * gjennomgås jevnlig for å luke manglende contract av db skjema.
 */
class RapporterUnmappedKolonnerIDatabaseTest {
    private static final Logger LOG = LoggerFactory.getLogger(RapporterUnmappedKolonnerIDatabaseTest.class);

    private static EntityManagerFactory entityManagerFactory;

    public RapporterUnmappedKolonnerIDatabaseTest() {
    }

    @BeforeAll
    public static void setup() {
        // Kan ikke skrus på nå - trigger på CHAR kolonner som kunne vært VARCHAR. Må
        // fikses først
        // System.setProperty("hibernate.hbm2ddl.auto", "validate");
        try {
            Databaseskjemainitialisering.settJdniOppslag();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }

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
        var groupingBy = Collectors.groupingBy((Object[] cols) -> ((String) cols[0]).toUpperCase(), TreeMap::new,
                Collectors.mapping((Object[] cols) -> ((String) cols[1]).toUpperCase(), Collectors.toCollection(TreeSet::new)));

        var em = entityManagerFactory.createEntityManager();
        try {
            if (namespace == null) {
                return (NavigableMap<String, Set<String>>) em
                        .createNativeQuery(
                                "select table_name, column_name from all_tab_cols where owner=SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND virtual_column='NO' AND hidden_column='NO'")
                        .getResultStream()
                        .collect(groupingBy);
            }
            return (NavigableMap<String, Set<String>>) em
                    .createNativeQuery(
                            "select table_name, column_name from all_tab_cols where owner=:ns AND virtual_column='NO' AND hidden_column='NO'")
                    .setParameter("ns", namespace)
                    .getResultStream()
                    .collect(groupingBy);
        } finally {
            em.close();
        }
    }

    @Test
    void sjekk_unmapped() {
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
                var columns = (List<Column>) StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(
                                table.getColumnIterator(),
                                Spliterator.ORDERED),
                        false)
                        .collect(Collectors.toList());

                var columnNames = columns.stream().map(c -> c.getName().toUpperCase()).collect(Collectors.toCollection(TreeSet::new));
                var tableName = table.getName().toUpperCase();
                if (dbColumns.containsKey(tableName)) {
                    var unmapped = new TreeSet<>(dbColumns.get(tableName));
                    unmapped.removeAll(columnNames);
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
