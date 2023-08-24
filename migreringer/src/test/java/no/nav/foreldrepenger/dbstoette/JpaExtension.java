package no.nav.foreldrepenger.dbstoette;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.foreldrepenger.dbstoette.Databaseskjemainitialisering.initUnitTestDataSource;
import static no.nav.foreldrepenger.dbstoette.Databaseskjemainitialisering.migrerUnittestSkjemaer;

public class JpaExtension extends EntityManagerAwareExtension {

    private static final Logger LOG = LoggerFactory.getLogger(JpaExtension.class);
    static {
        if (Environment.current().getProperty("maven.cmd.line.args") == null) {
            LOG.info("Kjører IKKE under maven");
            // prøver alltid migrering hvis endring, ellers funker det dårlig i IDE.
            migrerUnittestSkjemaer();
        }
        initUnitTestDataSource();
    }

}
