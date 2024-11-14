package no.nav.foreldrepenger.dbstoette;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareExtension;

import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class JpaExtension extends EntityManagerAwareExtension {

    public static final String DEFAULT_TEST_DB_SCHEMA_NAME;
    private static final String TEST_DB_CONTAINER = Environment.current().getProperty("testcontainer.test.db", String.class, "gvenzl/oracle-free:23-slim-faststart");
    private static final OracleContainer TEST_DATABASE;

    static {
        TEST_DATABASE = new OracleContainer(DockerImageName.parse(TEST_DB_CONTAINER))
            .withCopyFileToContainer(MountableFile.forHostPath("../../.oracle/oracle-init/fpsak.sql"), "/docker-entrypoint-initdb.d/init.sql")
            .withReuse(true);
        TEST_DATABASE.start();
        DEFAULT_TEST_DB_SCHEMA_NAME = TEST_DATABASE.getUsername();
        TestDatabaseInit.settOppDatasourceOgMigrer(TEST_DATABASE.getJdbcUrl(), "fpsak_hist", "fpsak_hist", "dvhDS");
        TestDatabaseInit.settOppDatasourceOgMigrer(TEST_DATABASE.getJdbcUrl(), "fpsak", "fpsak", "defaultDS");
    }
}
