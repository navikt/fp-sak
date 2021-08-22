package no.nav.foreldrepenger.web.app.healthchecks;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.liveness.KafkaIntegration;
import no.nav.foreldrepenger.web.app.healthchecks.checks.DatabaseHealthCheck;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
public class Selftests {

    private DatabaseHealthCheck databaseHealthCheck;
    private List<KafkaIntegration> kafkaList = new ArrayList<>();

    private boolean isDatabaseReady;
    private LocalDateTime sistOppdatertTid = LocalDateTime.now().minusDays(1);

    private String applicationName;
    private SelftestResultat selftestResultat;

    @Inject
    public Selftests(DatabaseHealthCheck databaseHealthCheck,
                     @Any Instance<KafkaIntegration> kafkaIntegrations,
                     @KonfigVerdi(value = "application.name") String applicationName) {
        this.databaseHealthCheck = databaseHealthCheck;
        kafkaIntegrations.forEach(this.kafkaList::add);
        this.applicationName = applicationName;
    }

    Selftests() {
        // for CDI proxy
    }

    public SelftestResultat run() {
        oppdaterSelftestResultatHvisNødvendig();
        return selftestResultat; // NOSONAR
    }

    public boolean isReady() {
        // Bruk denne for NAIS-respons og skill omfanget her.
        oppdaterSelftestResultatHvisNødvendig();
        return isDatabaseReady; // NOSONAR
    }

    public boolean isKafkaAlive() {
        return kafkaList.stream().allMatch(KafkaIntegration::isAlive);
    }

    private synchronized void oppdaterSelftestResultatHvisNødvendig() {
        if (sistOppdatertTid.isBefore(LocalDateTime.now().minusSeconds(30))) {
            isDatabaseReady = databaseHealthCheck.isOK();
            selftestResultat = innhentSelftestResultat();
            sistOppdatertTid = LocalDateTime.now();
        }
    }

    private SelftestResultat innhentSelftestResultat() {
        var samletResultat = new SelftestResultat();
        samletResultat.setApplication(applicationName);
        samletResultat.setTimestamp(LocalDateTime.now());

        samletResultat.leggTilResultatForKritiskTjeneste(isDatabaseReady, databaseHealthCheck.getDescription(), databaseHealthCheck.getEndpoint());

        // TODO Sjekk mot ABAKUS - kritisk for operasjonen og feiler mest regelmessig pga Skatt, deretter oppdrag kveldstid
        // Potensielt to ulike formål: Trafikkstyring via nais isReady + Visning til SBH i GUI om årsaker
        // Da er skatt/inntekt/tilbake forståelig - men ikke masse interne koder ...
        // Tenk kreativt - fx sjekk appene sin isReady via SD/<app>>/internal/health/isReady = 200 / 503
        return samletResultat;
    }

}
