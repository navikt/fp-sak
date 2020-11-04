package no.nav.foreldrepenger.økonomi.grensesnittavstemming;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchStatus;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.økonomi.grensesnittavstemming.queue.producer.GrensesnittavstemmingJmsProducer;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomioppdragRepository;

/**
 * Produserer en avstemmingsfil på XML-format som skal brukes i eksisterende grensesnitt for avstemmingskomponent i økonomisystemet.
 * Informasjonen i avstemmingsmelding hentes ut fra økonomilageret.
 */

@ApplicationScoped
public class GrensesnittavstemmingBatchTjeneste implements BatchTjeneste {

    private static final String BATCHNAVN = "BVL001";
    private static final Logger log = LoggerFactory.getLogger(GrensesnittavstemmingBatchTjeneste.class);
    private ØkonomioppdragRepository økonomioppdragRepository;
    private GrensesnittavstemmingJmsProducer grensesnittavstemmingJmsProducer;

    @Inject
    public GrensesnittavstemmingBatchTjeneste(ØkonomioppdragRepository økonomioppdragRepository,
                                              GrensesnittavstemmingJmsProducer grensesnittavstemmingJmsProducer) {
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.grensesnittavstemmingJmsProducer = grensesnittavstemmingJmsProducer;
    }

    private void utførGrensesnittavstemming(LocalDate fomDato, LocalDate tomDato, String fagområde) {
        List<Oppdrag110> oppdragsliste = økonomioppdragRepository.hentOppdrag110ForPeriodeOgFagområde(fomDato, tomDato, fagområde);
        if (oppdragsliste.isEmpty()) {
            log.info("Ingen oppdrag funnet for periode {} - {} for fagområde {}. Grensesnittavstemming ikke utført.", fomDato, tomDato, fagområde); //NOSONAR
            return;
        }
        GrensesnittavstemmingMapper mapper = new GrensesnittavstemmingMapper(oppdragsliste, fagområde);
        log.info("Starter grensesnittavstemming med id: {} for periode: {} - {} for fagområde {}. {} oppdrag funnet. ", mapper.getAvstemmingId(), fomDato, tomDato, fagområde, oppdragsliste.size()); //NOSONAR
        String startmelding = mapper.lagStartmelding();
        logMelding("startmelding", startmelding);
        List<String> datameldinger = mapper.lagDatameldinger();
        for (String datamelding : datameldinger) {
            logMelding("datamelding", datamelding);
        }
        String sluttmelding = mapper.lagSluttmelding();
        logMelding("sluttmelding", sluttmelding);
        grensesnittavstemmingJmsProducer.sendGrensesnittavstemming(startmelding);
        for (String datamelding : datameldinger) {
            grensesnittavstemmingJmsProducer.sendGrensesnittavstemming(datamelding);
        }
        grensesnittavstemmingJmsProducer.sendGrensesnittavstemming(sluttmelding);
        log.info("Fullført grensesnittavstemming med id: {}", mapper.getAvstemmingId()); //NOSONAR
    }

    private void logMelding(String meldingtype, String melding) {
        log.info("Opprettet {} med lengde {} tegn", meldingtype, melding.length()); //NOSONAR
    }

    @Override
    public GrensesnittavstemmingBatchArguments createArguments(Map<String, String> jobArguments) {
        return new GrensesnittavstemmingBatchArguments(jobArguments);
    }

    @Override
    public String launch(BatchArguments arguments) {
        GrensesnittavstemmingBatchArguments batchArguments = (GrensesnittavstemmingBatchArguments) arguments; // NOSONAR
        utførGrensesnittavstemming(batchArguments.getFom(), batchArguments.getTom(), batchArguments.getFagområde());
        return BATCHNAVN + "-" + UUID.randomUUID();
    }

    @Override
    public BatchStatus status(String batchInstanceNumber) {
        // Antar her at alt har gått bra siden denne er en synkron jobb.
        return BatchStatus.OK;
    }

    @Override
    public String getBatchName() {
        return BATCHNAVN;
    }
}
