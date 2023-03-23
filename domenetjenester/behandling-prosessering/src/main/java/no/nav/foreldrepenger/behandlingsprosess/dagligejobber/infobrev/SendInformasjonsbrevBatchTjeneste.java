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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

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
    public SendInformasjonsbrevBatchArguments createArguments(Map<String, String> jobArguments) {
        return new SendInformasjonsbrevBatchArguments(jobArguments, 4);
    }

    @Override
    public String launch(BatchArguments arguments) {
        var batchArguments = (SendInformasjonsbrevBatchArguments) arguments;
        var saker = informasjonssakRepository.finnSakerMedInnvilgetMaksdatoInnenIntervall(batchArguments.getFom(),
                batchArguments.getTom());
        var baseline = LocalTime.now();
        saker.forEach(sak -> {
            LOG.info("Oppretter informasjonssak-task for {}", sak.getAktørId().getId());
            var data = ProsessTaskData.forProsessTask(OpprettInformasjonsFagsakTask.class);
            data.setAktørId(sak.getAktørId().getId());
            data.setCallIdFraEksisterende();
            data.setNesteKjøringEtter(LocalDateTime.of(LocalDate.now(), baseline.plusSeconds(LocalDateTime.now().getNano() % 419)));
            data.setPrioritet(100);
            data.setProperty(OpprettInformasjonsFagsakTask.OPPRETTET_DATO_KEY, sak.getKildesakOpprettetDato().toString());
            data.setProperty(OpprettInformasjonsFagsakTask.FH_DATO_KEY, sak.getFamilieHndelseDato().toString());
            data.setProperty(OpprettInformasjonsFagsakTask.BEH_ENHET_ID_KEY, sak.getEnhet());
            data.setProperty(OpprettInformasjonsFagsakTask.BEHANDLING_AARSAK, BehandlingÅrsakType.INFOBREV_BEHANDLING.getKode());
            data.setProperty(OpprettInformasjonsFagsakTask.FAGSAK_ID_MOR_KEY, sak.getKildeFagsakId().toString());
            taskTjeneste.lagre(data);
        });
        return BATCHNAVN + "-" + saker.size();
    }

    @Override
    public String getBatchName() {
        return BATCHNAVN;
    }
}
