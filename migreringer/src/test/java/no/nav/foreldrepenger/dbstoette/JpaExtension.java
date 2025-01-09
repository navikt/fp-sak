package no.nav.foreldrepenger.dbstoette;

import java.io.File;

import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareExtension;

public class JpaExtension extends EntityManagerAwareExtension {

    public static final String DEFAULT_TEST_DB_SCHEMA_NAME;
    private static final String TEST_DB_CONTAINER = Environment.current().getProperty("testcontainer.test.db", String.class, "gvenzl/oracle-free:23-slim-faststart");
    private static final OracleContainer TEST_DATABASE;
    private static final String INIT_SCRIPT_PATH = "../.oracle/oracle-init/fpsak.sql";

    static {
        var initPath = INIT_SCRIPT_PATH;
        while (!(new File(initPath)).exists()) {
            initPath = "../" + initPath;
        }
        TEST_DATABASE = new OracleContainer(DockerImageName.parse(TEST_DB_CONTAINER))
            .withCopyFileToContainer(MountableFile.forHostPath(initPath), "/docker-entrypoint-initdb.d/init.sql")
            .withReuse(true);
        TEST_DATABASE.start();
        DEFAULT_TEST_DB_SCHEMA_NAME = TEST_DATABASE.getUsername();
        var ds = TestDatabaseInit.settOppDatasourceOgMigrer(TEST_DATABASE.getJdbcUrl(), "fpsak", "fpsak", TestDatabaseInit.DEFAULT_DS_SCHEMA);
        TestDatabaseInit.settOppDatasourceOgMigrer(TEST_DATABASE.getJdbcUrl(), "fpsak_hist", "fpsak_hist", "dvhDS");
        TestDatabaseInit.settJdniOppslag(ds);
    }
}
