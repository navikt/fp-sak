package no.nav.foreldrepenger.dbstoette;

import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareExtension;

public class FPsakEntityManagerAwareExtension extends EntityManagerAwareExtension {

    @Override
    protected void init() {
        if (System.getenv("MAVEN_CMD_LINE_ARGS") == null) {
            // prøver alltid migrering hvis endring, ellers funker det dårlig i IDE.
            Databaseskjemainitialisering.migrerUnittestSkjemaer();
        }

        Databaseskjemainitialisering.settPlaceholdereOgJdniOppslag();
    }

}
