package no.nav.foreldrepenger.web.app.startupinfo;

import static com.google.common.collect.Maps.fromProperties;
import static java.util.Map.Entry.comparingByKey;
import static no.nav.vedtak.konfig.StandardPropertySource.APP_PROPERTIES;
import static no.nav.vedtak.konfig.StandardPropertySource.ENV_PROPERTIES;
import static no.nav.vedtak.konfig.StandardPropertySource.SYSTEM_PROPERTIES;

import java.util.List;
import java.util.Map.Entry;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.jboss.resteasy.annotations.Query;
import org.jboss.weld.util.reflection.Formats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;

import no.nav.foreldrepenger.web.app.healthchecks.Selftests;
import no.nav.foreldrepenger.web.app.healthchecks.checks.ExtHealthCheck;
import no.nav.vedtak.konfig.StandardPropertySource;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.util.env.Environment;

/** Dependent scope siden vi lukker denne når vi er ferdig. */
@Dependent
class AppStartupInfoLogger {

    private static final List<String> SECRETS = List.of("passord", "password", "passwd");

    private static final Logger LOG = LoggerFactory.getLogger(AppStartupInfoLogger.class);

    private Selftests selftests;

    private static final String OPPSTARTSINFO = "OPPSTARTSINFO";
    private static final String HILITE_SLUTT = "********";
    private static final String HILITE_START = HILITE_SLUTT;
    private static final String KONFIGURASJON = "Konfigurasjon";
    private static final String SELFTEST = "Selftest";
    private static final String APPLIKASJONENS_STATUS = "Applikasjonens status";
    private static final String START = "start:";
    private static final String SLUTT = "slutt.";

    private static final List<String> IGNORE = List.of("TCP_ADDR", "PORT_HTTP", "SERVICE_HOST",
            "TCP_PROTO", "_TCP", "_PORT");

    private static final Environment ENV = Environment.current();

    @Inject
    AppStartupInfoLogger(Selftests selftests) {
        this.selftests = selftests;
    }

    void logAppStartupInfo() {
        log(HILITE_START + " " + OPPSTARTSINFO + " " + START + " " + HILITE_SLUTT);
        logVersjoner();
        logKonfigurasjon();
        logSelftest();
        log(HILITE_START + " " + OPPSTARTSINFO + " " + SLUTT + " " + HILITE_SLUTT);
    }

    private void logKonfigurasjon() {
        log(KONFIGURASJON + " " + START);
        log(SYSTEM_PROPERTIES);
        log(ENV_PROPERTIES);
        log(APP_PROPERTIES);
        log(KONFIGURASJON + " " + SLUTT);
    }

    private void log(StandardPropertySource source) {
        fromProperties(ENV.getProperties(source).getVerdier()).entrySet()
                .stream()
                .sorted(comparingByKey())
                .forEach(e -> log(source, e));
    }

    private void logSelftest() {
        log(SELFTEST + " " + START);

        // callId er påkrevd på utgående kall og må settes før selftest kjøres
        MDCOperations.putCallId();
        var samletResultat = selftests.run();
        MDCOperations.removeCallId();

        samletResultat.getAlleResultater().stream()
                .forEach(AppStartupInfoLogger::log);

        log(APPLIKASJONENS_STATUS + ": {}", samletResultat.getAggregateResult());
        log(SELFTEST + " " + SLUTT);
    }

    private static void log(StandardPropertySource source, Entry<String, String> entry) {
        String value = secret(entry.getKey()) ? hide(entry.getValue()) : entry.getValue();
        log(ignore(entry.getKey()), "{}: {}={}", source.getName(), entry.getKey(), value);
    }

    private static boolean ignore(String key) {
        return matchEndsWith(key, IGNORE);
    }

    private static boolean secret(String key) {
        return matchEndsWith(key, SECRETS);
    }

    private static boolean matchEndsWith(String key, List<String> elems) {
        for (String elem : elems) {
            if (key.toLowerCase().endsWith(elem.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static String hide(String val) {
        return "*".repeat(val.length());
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

    private static void log(HealthCheck.Result result) {
        if (result.getDetails() != null) {
            OppstartFeil.FACTORY.selftestStatus(
                    getStatus(result.isHealthy()),
                    (String) result.getDetails().get(ExtHealthCheck.DETAIL_DESCRIPTION),
                    (String) result.getDetails().get(ExtHealthCheck.DETAIL_ENDPOINT),
                    (String) result.getDetails().get(ExtHealthCheck.DETAIL_RESPONSE_TIME),
                    result.getMessage()).log(LOG);
        } else {
            OppstartFeil.FACTORY.selftestStatus(
                    getStatus(result.isHealthy()),
                    null,
                    null,
                    null,
                    result.getMessage()).log(LOG);
        }
    }

    private static String getStatus(boolean isHealthy) {
        return isHealthy ? "OK" : "ERROR";
    }

    private void logVersjoner() {
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
