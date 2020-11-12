package no.nav.foreldrepenger.dbstoette;

import static no.nav.foreldrepenger.dbstoette.Databaseskjemainitialisering.migrer;
import static no.nav.foreldrepenger.dbstoette.Databaseskjemainitialisering.settJdniOppslag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareExtension;

public class FPsakEntityManagerAwareExtension extends EntityManagerAwareExtension {

    private static final Logger LOG = LoggerFactory.getLogger(FPsakEntityManagerAwareExtension.class);

    static {
        if (!DBTestUtil.kjøresAvMaven()) {
            LOG.info("Kjører IKKE under maven");
            // prøver alltid migrering hvis endring, ellers funker det dårlig i IDE.
            migrer();
        }
        settJdniOppslag();
    }

}
