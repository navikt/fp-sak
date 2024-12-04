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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class SendInformasjonsbrevBatchTjeneste implements BatchTjeneste {

    static final String BATCHNAVN = "BVL008";
    private static final Logger LOG = LoggerFactory.getLogger(SendInformasjonsbrevBatchTjeneste.class);
    private InformasjonssakRepository informasjonssakRepository;
    private ProsessTaskTjeneste taskTjeneste;

    @Inject
    public SendInformasjonsbrevBatchTjeneste(InformasjonssakRepository informasjonssakRepository, ProsessTaskTjeneste taskTjeneste) {
        this.informasjonssakRepository = informasjonssakRepository;
        this.taskTjeneste = taskTjeneste;
    }

    @Override
    public String launch(Properties properties) {
        var periode = lagPeriodeEvtOppTilIDag(properties, Period.ofWeeks(4));
        if (DAYS.between(periode.fom(), periode.tom()) > 366) {
            throw new IllegalArgumentException("Informasjonsbrev: For mange dager");
        }
        var saker = informasjonssakRepository.finnSakerMedInnvilgetMaksdatoInnenIntervall(periode.fom(), periode.tom());
        var baseline = LocalTime.now();
        var callId = Optional.ofNullable(MDCOperations.getCallId()).orElseGet(MDCOperations::generateCallId);
        saker.forEach(sak -> {
            LOG.info("Oppretter informasjonssak-task for {}", sak.getAktørId());
            var data = ProsessTaskData.forProsessTask(OpprettInformasjonsFagsakTask.class);
            data.setAktørId(sak.getAktørId().getId());
            data.setNesteKjøringEtter(LocalDateTime.of(LocalDate.now(), baseline.plusSeconds(LocalDateTime.now().getNano() % 419)));
            data.setProperty(OpprettInformasjonsFagsakTask.OPPRETTET_DATO_KEY, sak.getKildesakOpprettetDato().toString());
            data.setProperty(OpprettInformasjonsFagsakTask.FH_DATO_KEY, sak.getFamilieHndelseDato().toString());
            data.setProperty(OpprettInformasjonsFagsakTask.BEH_ENHET_ID_KEY, sak.getEnhet());
            data.setProperty(OpprettInformasjonsFagsakTask.BEHANDLING_AARSAK, BehandlingÅrsakType.INFOBREV_BEHANDLING.getKode());
            data.setProperty(OpprettInformasjonsFagsakTask.FAGSAK_ID_MOR_KEY, sak.getKildeFagsakId().toString());
            data.setCallId(callId + "_" + sak.getKildeFagsakId());
            taskTjeneste.lagre(data);
        });
        return BATCHNAVN + "-" + saker.size();
    }

    @Override
    public String getBatchName() {
        return BATCHNAVN;
    }
}
