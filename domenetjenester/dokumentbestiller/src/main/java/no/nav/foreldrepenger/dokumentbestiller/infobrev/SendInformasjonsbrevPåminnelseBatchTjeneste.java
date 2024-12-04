package no.nav.foreldrepenger.dokumentbestiller.infobrev;

import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.Optional;
import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class SendInformasjonsbrevPåminnelseBatchTjeneste implements BatchTjeneste {

    static final String BATCHNAVN = "BVL011";
    private static final Logger LOG = LoggerFactory.getLogger(SendInformasjonsbrevPåminnelseBatchTjeneste.class);
    private InformasjonssakRepository informasjonssakRepository;
    private ProsessTaskTjeneste taskTjeneste;

    @Inject
    public SendInformasjonsbrevPåminnelseBatchTjeneste(InformasjonssakRepository informasjonssakRepository, ProsessTaskTjeneste taskTjeneste) {
        this.informasjonssakRepository = informasjonssakRepository;
        this.taskTjeneste = taskTjeneste;
    }

    @Override
    public String launch(Properties properties) {
        var periode = lagPeriodeEvtOppTilIDag(properties, Period.ofWeeks(-130));
        if (DAYS.between(periode.fom(), periode.tom()) > 366) {
            throw new IllegalArgumentException("Informasjonsbrev påminnelse: For mange dager");
        }
        LOG.info("Ser etter aktuelle saker med barn født mellom {} og {}", periode.fom(), periode.tom());
        var saker = informasjonssakRepository.finnSakerDerMedforelderIkkeHarSøktOgBarnetBleFødtInnenforIntervall(periode.fom(), periode.tom());
        var callId = Optional.ofNullable(MDCOperations.getCallId()).orElseGet(MDCOperations::generateCallId);
        var baseline = LocalTime.now();
        saker.forEach(sak -> {
            LOG.info("Oppretter informasjonsbrev-påminnelse task for {}", sak.saksnummer().getVerdi());
            var data = ProsessTaskData.forProsessTask(SendInformasjonsbrevPåminnelseTask.class);
            data.setNesteKjøringEtter(LocalDateTime.of(LocalDate.now(), baseline.plusSeconds(LocalDateTime.now().getNano() % 419)));
            data.setFagsak(sak.saksnummer().getVerdi(), sak.fagsakId());
            data.setCallId(callId + "_" + sak.fagsakId());
            taskTjeneste.lagre(data);
        });
        return BATCHNAVN + "-" + saker.size();
    }

    @Override
    public String getBatchName() {
        return BATCHNAVN;
    }
}
