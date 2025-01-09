package no.nav.foreldrepenger.dbstoette;

import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareExtension;

public class JpaExtension extends EntityManagerAwareExtension {

    private static final OracleContainer TEST_DATABASE;

    static {
        TEST_DATABASE = new OracleContainer(DockerImageName.parse(TestDatabaseInit.TEST_DB_CONTAINER))
            .withCopyFileToContainer(MountableFile.forHostPath(TestDatabaseInit.DB_SETUP_SCRIPT_PATH), "/docker-entrypoint-initdb.d/init.sql")
            .withReuse(true);
        TEST_DATABASE.start();
        var ds = TestDatabaseInit.settOppDatasourceOgMigrer(TEST_DATABASE.getJdbcUrl(), "fpsak", "fpsak", TestDatabaseInit.DEFAULT_DS_SCHEMA);
        TestDatabaseInit.settOppDatasourceOgMigrer(TEST_DATABASE.getJdbcUrl(), "fpsak_hist", "fpsak_hist", "dvhDS");
        TestDatabaseInit.settJdniOppslag(ds);
    }
}
