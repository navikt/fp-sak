package no.nav.foreldrepenger.web.app.healthchecks;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;

import no.nav.foreldrepenger.domene.liveness.KafkaIntegration;
import no.nav.foreldrepenger.web.app.healthchecks.checks.ExtHealthCheck;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class Selftests {

    private static final Logger LOGGER = LoggerFactory.getLogger(Selftests.class);

    private HealthCheckRegistry registry;
    private Map<String, Boolean> erKritiskTjeneste = new HashMap<>();

    private Instance<ExtHealthCheck> healthChecks;

    private List<KafkaIntegration> kafkaList = new ArrayList<>();

    private boolean hasSetupChecks;

    private String applicationName;

    private static final String BUILD_PROPERTIES = "build.properties";

    private SelftestResultat selftestResultat;
    private LocalDateTime sistOppdatertTid = LocalDateTime.now().minusDays(1);

    @Inject
    public Selftests(HealthCheckRegistry registry,
                     @Any Instance<ExtHealthCheck> healthChecks,
                     @Any Instance<KafkaIntegration> kafkaIntegrations,
                     @KonfigVerdi(value = "application.name") String applicationName) {

        this.registry = registry;
        this.healthChecks = healthChecks;
        this.applicationName = applicationName;
        kafkaIntegrations.forEach(this.kafkaList::add);
    }

    Selftests() {
        // for CDI proxy
    }

    public SelftestResultat run() {
        oppdaterSelftestResultatHvisNødvendig();
        return selftestResultat; // NOSONAR
    }

    public boolean isKafkaAlive() {
        return kafkaList.stream().allMatch(KafkaIntegration::isAlive);
    }

    private synchronized void oppdaterSelftestResultatHvisNødvendig() {
        if (sistOppdatertTid.isBefore(LocalDateTime.now().minusSeconds(30))) {
            selftestResultat = innhentSelftestResultat();
            sistOppdatertTid = LocalDateTime.now();
        }
    }

    private SelftestResultat innhentSelftestResultat() {
        setupChecks();

        SelftestResultat samletResultat = new SelftestResultat();
        populateBuildtimeProperties(samletResultat);
        samletResultat.setTimestamp(LocalDateTime.now());

        for (String name : registry.getNames()) {
            HealthCheck.Result result = registry.runHealthCheck(name);
            if (erKritiskTjeneste.get(name)) {
                samletResultat.leggTilResultatForKritiskTjeneste(result);
            } else {
                samletResultat.leggTilResultatForIkkeKritiskTjeneste(result);
            }
        }
        return samletResultat;
    }

    private synchronized void setupChecks() {
        if (!hasSetupChecks) {
            for (ExtHealthCheck healthCheck : healthChecks) {
                if (healthCheck.getClass().isAnnotationPresent(Dependent.class)) {
                    throw new IllegalStateException(
                        "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + healthCheck.getClass());
                }
                registrer(healthCheck);
            }
            hasSetupChecks = true;
        }
    }

    private void registrer(ExtHealthCheck healthCheck) {
        String name = healthCheck.getClass().getName();
        if (erKritiskTjeneste.containsKey(name)) {
            throw SelftestFeil.FACTORY.dupliserteSelftestNavn(name).toException();
        }
        registry.register(name, healthCheck);
        erKritiskTjeneste.put(name, healthCheck.erKritiskTjeneste());
    }

    private void populateBuildtimeProperties(SelftestResultat samletResultat) {
        String version = null;
        String revision = null;
        String timestamp = null;

        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(BUILD_PROPERTIES)) {
            Properties prop = new Properties();
            if (is == null && Boolean.getBoolean("develop-local")) {
                // det er forventet at build.properties-filen ikke er tilgjengelig lokalt.
                // unngår derfor å forsøke å lese den.
                return;
            }
            prop.load(is);
            version = prop.getProperty("version");
            revision = prop.getProperty("revision");
            timestamp = prop.getProperty("timestamp");
        } catch (IOException e) {
            SelftestFeil.FACTORY.klarteIkkeÅLeseBuildTimePropertiesFil(e).log(LOGGER);
            // Ikke re-throw - dette er ikke kritisk
        }

        samletResultat.setVersion(buildtimePropertyValueIfNull(version));
        samletResultat.setApplication(applicationName);
        samletResultat.setRevision(buildtimePropertyValueIfNull(revision));
        samletResultat.setBuildTime(buildtimePropertyValueIfNull(timestamp));
    }

    private String buildtimePropertyValueIfNull(String value) {
        String newValue = value;
        if (newValue == null) {
            newValue = "?.?.?";
        }
        return newValue;
    }
}
