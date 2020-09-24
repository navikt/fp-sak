package no.nav.foreldrepenger.dbstoette;

import static no.nav.foreldrepenger.dbstoette.Databaseskjemainitialisering.settPlaceholdereOgJdniOppslag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareExtension;
import no.nav.vedtak.util.env.Environment;

public class FPsakEntityManagerAwareExtension extends EntityManagerAwareExtension {

    private static final Logger LOG = LoggerFactory.getLogger(FPsakEntityManagerAwareExtension.class);
    private static final boolean isNotRunningUnderMaven = Environment.current().getProperty("maven.cmd.line.args") == null;

    static {
        if (isNotRunningUnderMaven) {
            LOG.info("Kjører IKKE under maven");
            // prøver alltid migrering hvis endring, ellers funker det dårlig i IDE.
            // migrerUnittestSkjemaer();
        }
        LOG.info("Placeholders");
        settPlaceholdereOgJdniOppslag();
    }

}
