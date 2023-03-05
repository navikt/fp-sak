package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

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
    public SendInformasjonsbrevBatchArguments createArguments(Map<String, String> jobArguments) {
        return new SendInformasjonsbrevBatchArguments(jobArguments, -130); // Ser etter fødsler som skjedde 130 uker tilbake (ca 2.5 år)
    }

    @Override
    public String launch(BatchArguments arguments) {
        var batchArguments = (SendInformasjonsbrevBatchArguments) arguments;
        LOG.info("Ser etter aktuelle saker med barn født mellom {} og {}", batchArguments.getFom(), batchArguments.getTom());
        var saker = informasjonssakRepository.finnSakerDerMedforelderIkkeHarSøktOgBarnetBleFødtInnenforIntervall(
            batchArguments.getFom(), batchArguments.getTom());
        var baseline = LocalTime.now();
        saker.forEach(sak -> {
            LOG.info("Oppretter informasjonsbrev-påminnelse task for {}", sak.getAktørId().getId());
            var data = ProsessTaskData.forProsessTask(SendInformasjonsbrevPåminnelseTask.class);
            data.setAktørId(sak.getAktørId().getId());
            data.setCallIdFraEksisterende();
            data.setNesteKjøringEtter(LocalDateTime.of(LocalDate.now(), baseline.plusSeconds(LocalDateTime.now().getNano() % 419)));
            data.setPrioritet(100);
            data.setProperty(SendInformasjonsbrevPåminnelseTask.BEH_ENHET_ID_KEY, sak.getEnhet());
            data.setProperty(SendInformasjonsbrevPåminnelseTask.BEH_ENHET_NAVN_KEY, sak.getEnhetNavn());
            data.setProperty(SendInformasjonsbrevPåminnelseTask.FAGSAK_ID_KEY, sak.getKildeFagsakId().toString());
            taskTjeneste.lagre(data);
        });
        return BATCHNAVN + "-" + saker.size();
    }

    @Override
    public String getBatchName() {
        return BATCHNAVN;
    }
}
