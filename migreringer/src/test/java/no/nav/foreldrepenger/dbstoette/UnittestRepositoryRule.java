package no.nav.foreldrepenger.dbstoette;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

import java.util.TimeZone;

public class UnittestRepositoryRule extends RepositoryRule {
    private static final Logger log = LoggerFactory.getLogger(UnittestRepositoryRule.class);

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Oslo"));
        if (System.getenv("MAVEN_CMD_LINE_ARGS") == null) {
            // prøver alltid migrering hvis endring, ellers funker det dårlig i IDE.
            log.warn("Kjører migreringer");
            Databaseskjemainitialisering.migrerUnittestSkjemaer();
        } else {
            // Maven kjører testen
            // kun kjør migreringer i migreringer modul
        }

        Databaseskjemainitialisering.settPlaceholdereOgJdniOppslag();

    }

    public UnittestRepositoryRule() {
        super();
    }

    @Override
    protected void init() {
    }

}
