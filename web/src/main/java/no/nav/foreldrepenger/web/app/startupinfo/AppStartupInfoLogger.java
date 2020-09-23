package no.nav.foreldrepenger.web.app.startupinfo;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.jboss.resteasy.annotations.Query;
import org.jboss.weld.util.reflection.Formats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.web.app.healthchecks.SelftestResultat;
import no.nav.foreldrepenger.web.app.healthchecks.Selftests;
import no.nav.vedtak.log.mdc.MDCOperations;

/** Dependent scope siden vi lukker denne når vi er ferdig. */
@Dependent
class AppStartupInfoLogger {

    private static final Logger LOG = LoggerFactory.getLogger(AppStartupInfoLogger.class);

    private Selftests selftests;

    private static final String OPPSTARTSINFO = "OPPSTARTSINFO";
    private static final String HILITE_SLUTT = "********";
    private static final String HILITE_START = HILITE_SLUTT;
    private static final String SELFTEST = "Selftest";
    private static final String APPLIKASJONENS_STATUS = "Applikasjonens status";
    private static final String START = "start:";
    private static final String SLUTT = "slutt.";

    @Inject
    AppStartupInfoLogger(Selftests selftests) {
        this.selftests = selftests;
    }

    void logAppStartupInfo() {
        log(HILITE_START + " " + OPPSTARTSINFO + " " + START + " " + HILITE_SLUTT);
        logVersjoner();
        logSelftest();
        log(HILITE_START + " " + OPPSTARTSINFO + " " + SLUTT + " " + HILITE_SLUTT);
    }

    private void logSelftest() {
        log(SELFTEST + " " + START);

        // callId er påkrevd på utgående kall og må settes før selftest kjøres
        MDCOperations.putCallId();
        var samletResultat = selftests.run();
        MDCOperations.removeCallId();

        samletResultat.getAlleResultater().forEach(AppStartupInfoLogger::log);

        log(APPLIKASJONENS_STATUS + ": {}", samletResultat.getAggregateResult());
        log(SELFTEST + " " + SLUTT);
    }

    private static void log(String msg, Object... args) {
        log(false, msg, args);
    }

    private static void log(boolean ignore, String msg, Object... args) {
        if (ignore) {
            LOG.debug(msg, args);
        } else {
            LOG.info(msg, args);
        }
    }

    private static void log(SelftestResultat.InternalResult result) {
        OppstartFeil.FACTORY.selftestStatus(
            getStatus(result.isReady()),
            result.getDescription(),
            result.getEndpoint()).log(LOG);
    }

    private static String getStatus(boolean isHealthy) {
        return isHealthy ? "OK" : "ERROR";
    }

    private static void logVersjoner() {
        // Noen biblioteker er bundlet med jboss og kan skape konflikter, eller jboss
        // overstyrer vår overstyring via modul classpath
        // her logges derfor hva som er effektivt tilgjengelig av ulike biblioteker som
        // kan være påvirket ved oppstart
        log("Bibliotek: Hibernate: {}", org.hibernate.Version.getVersionString());
        log("Bibliotek: Weld: {}", Formats.version(null));
        log("Bibliotek: CDI: {}", CDI.class.getPackage().getImplementationVendor() + ":"
                + CDI.class.getPackage().getSpecificationVersion());
        log("Bibliotek: Resteasy: {}", Query.class.getPackage().getImplementationVersion()); // tilfeldig valgt Resteasy
                                                                                             // klasse
    }
}
