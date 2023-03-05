package no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming.queue.producer.GrensesnittavstemmingJmsProducer;

/**
 * Produserer en avstemmingsfil på XML-format som skal brukes i eksisterende grensesnitt for avstemmingskomponent i økonomisystemet.
 * Informasjonen i avstemmingsmelding hentes ut fra økonomilageret.
 */

@ApplicationScoped
public class GrensesnittavstemmingBatchTjeneste implements BatchTjeneste {

    private static final String BATCHNAVN = "BVL001";
    private static final Logger LOG = LoggerFactory.getLogger(GrensesnittavstemmingBatchTjeneste.class);
    private ØkonomioppdragRepository økonomioppdragRepository;
    private GrensesnittavstemmingJmsProducer grensesnittavstemmingJmsProducer;

    @Inject
    public GrensesnittavstemmingBatchTjeneste(ØkonomioppdragRepository økonomioppdragRepository,
                                              GrensesnittavstemmingJmsProducer grensesnittavstemmingJmsProducer) {
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.grensesnittavstemmingJmsProducer = grensesnittavstemmingJmsProducer;
    }

    private void utførGrensesnittavstemming(LocalDate fomDato, LocalDate tomDato, String fagområde) {
        var oppdragsliste = økonomioppdragRepository.hentOppdrag110ForPeriodeOgFagområde(fomDato, tomDato, KodeFagområde.valueOf(fagområde));
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
    public GrensesnittavstemmingBatchArguments createArguments(Map<String, String> jobArguments) {
        return new GrensesnittavstemmingBatchArguments(jobArguments);
    }

    @Override
    public String launch(BatchArguments arguments) {
        var batchArguments = (GrensesnittavstemmingBatchArguments) arguments;
        utførGrensesnittavstemming(batchArguments.getFom(), batchArguments.getTom(), batchArguments.getFagområde());
        return BATCHNAVN + "-" + UUID.randomUUID();
    }

    @Override
    public String getBatchName() {
        return BATCHNAVN;
    }
}
