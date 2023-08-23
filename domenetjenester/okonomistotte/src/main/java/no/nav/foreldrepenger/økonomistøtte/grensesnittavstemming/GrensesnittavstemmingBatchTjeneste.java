package no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming.queue.producer.GrensesnittavstemmingJmsProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Properties;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Produserer en avstemmingsfil på XML-format som skal brukes i eksisterende grensesnitt for avstemmingskomponent i økonomisystemet.
 * Informasjonen i avstemmingsmelding hentes ut fra økonomilageret.
 */

@ApplicationScoped
public class GrensesnittavstemmingBatchTjeneste implements BatchTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(GrensesnittavstemmingBatchTjeneste.class);

    private static final String BATCHNAVN = "BVL001";

    private ØkonomioppdragRepository økonomioppdragRepository;
    private GrensesnittavstemmingJmsProducer grensesnittavstemmingJmsProducer;

    @Inject
    public GrensesnittavstemmingBatchTjeneste(ØkonomioppdragRepository økonomioppdragRepository,
                                              GrensesnittavstemmingJmsProducer grensesnittavstemmingJmsProducer) {
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.grensesnittavstemmingJmsProducer = grensesnittavstemmingJmsProducer;
    }

    private void utførGrensesnittavstemming(LocalDate fomDato, LocalDate tomDato, KodeFagområde fagområde) {
        var oppdragsliste = økonomioppdragRepository.hentOppdrag110ForPeriodeOgFagområde(fomDato, tomDato, fagområde);
        if (oppdragsliste.isEmpty()) {
            LOG.info("Ingen oppdrag funnet for periode {} - {} for fagområde {}. Grensesnittavstemming ikke utført.", fomDato, tomDato, fagområde);
            return;
        }
        var mapper = new GrensesnittavstemmingMapper(oppdragsliste, fagområde);
        LOG.info("Starter grensesnittavstemming med id: {} for periode: {} - {} for fagområde {}. {} oppdrag funnet. ", mapper.getAvstemmingId(), fomDato, tomDato, fagområde, oppdragsliste.size());
        var startmelding = mapper.lagStartmelding();
        logMelding("startmelding", startmelding);
        var datameldinger = mapper.lagDatameldinger();
        for (var datamelding : datameldinger) {
            logMelding("datamelding", datamelding);
        }
        var sluttmelding = mapper.lagSluttmelding();
        logMelding("sluttmelding", sluttmelding);
        grensesnittavstemmingJmsProducer.sendGrensesnittavstemming(startmelding);
        for (var datamelding : datameldinger) {
            grensesnittavstemmingJmsProducer.sendGrensesnittavstemming(datamelding);
        }
        grensesnittavstemmingJmsProducer.sendGrensesnittavstemming(sluttmelding);
        LOG.info("Fullført grensesnittavstemming med id: {}", mapper.getAvstemmingId());
    }

    private void logMelding(String meldingtype, String melding) {
        LOG.info("Opprettet {} med lengde {} tegn", meldingtype, melding.length());
    }

    @Override
    public String launch(Properties properties) {
        var fagområde = KodeFagområde.valueOf(properties.getProperty(FAGOMRÅDE_KEY));
        var periode = lagPeriodeEvtOppTilIDag(properties);
        if (DAYS.between(periode.fom(), periode.tom()) > 7) {
            throw new IllegalArgumentException("Grensesnittavstemming: For mange dager");
        }
        utførGrensesnittavstemming(periode.fom(), periode.tom(), fagområde);
        return BATCHNAVN + "-" + UUID.randomUUID();
    }

    @Override
    public String getBatchName() {
        return BATCHNAVN;
    }
}
