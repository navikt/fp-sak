package no.nav.foreldrepenger.dbstoette;

import java.io.File;

import javax.sql.DataSource;

import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.jpa.NamingStandard;
import no.nav.vedtak.felles.testutilities.db.MigrationUtil;

public final class TestDatabaseInit {

    private static final String TEST_DB_CONTAINER = Environment.current()
        .getProperty("testcontainer.test.db", String.class, "gvenzl/oracle-free:23-slim-faststart");

    private static final String INIT_SCRIPT_PATH = "../.oracle/oracle-init/fpsak.sql";

    private static final Environment ENV = Environment.current();

    private static DataSource ds;

    public static synchronized DataSource getDataSource() {
        if (!ENV.isLocal()) {
            throw new IllegalStateException("Forventer at denne migreringen bare kjøres lokalt");
        }
        if (ds == null) {
            var testDatabase = new OracleContainer(DockerImageName.parse(TEST_DB_CONTAINER)).withCopyFileToContainer(
                MountableFile.forHostPath(getTestDbSetupScriptPath()), "/docker-entrypoint-initdb.d/init.sql")
                .withReuse(true);
            testDatabase.start();
            ds = MigrationUtil.createLocalBuildTestDataSource(testDatabase.getJdbcUrl(), "fpsak", "fpsak");
            MigrationUtil.migrateLocalBuildTest(ds, getScriptLocation(NamingStandard.DEFAULT_DATA_SOURCE));
            try (var dvhDs = MigrationUtil.createLocalBuildTestDataSource(testDatabase.getJdbcUrl(), "fpsak_hist", "fpsak_hist")) {
                MigrationUtil.migrateLocalBuildTest(dvhDs, getScriptLocation("dvhDS"));
            }
        }
        return ds;
    }

    // Trengs ved multi-modul-prosjekt
    private static String getScriptLocation(String schema) {
        var relativePath = "migreringer/src/main/resources" + NamingStandard.DEFAULT_MIGRATION_ROOT + schema;
        return MigrationUtil.getScriptLocation(relativePath);
    }

    private static String getTestDbSetupScriptPath() {
        var initPath = INIT_SCRIPT_PATH;
        while (!(new File(initPath)).exists()) {
            initPath = "../" + initPath;
        }
        return initPath;
    }
}
