package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchStatus;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class SendInformasjonsbrevOppholdBatchTjeneste implements BatchTjeneste {

    static final String BATCHNAVN = "BVL009";
    private static final Logger log = LoggerFactory.getLogger(SendInformasjonsbrevOppholdBatchTjeneste.class);
    private InformasjonssakRepository informasjonssakRepository;
    private ProsessTaskRepository prosessTaskRepository;

    @Inject
    public SendInformasjonsbrevOppholdBatchTjeneste(InformasjonssakRepository informasjonssakRepository, ProsessTaskRepository prosessTaskRepository) {
        this.informasjonssakRepository = informasjonssakRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public SendInformasjonsbrevBatchArguments createArguments(Map<String, String> jobArguments) {
        return new SendInformasjonsbrevBatchArguments(jobArguments);
    }

    @Override
    public String launch(BatchArguments arguments) {
        SendInformasjonsbrevBatchArguments batchArguments = (SendInformasjonsbrevBatchArguments) arguments; // NOSONAR
        List<InformasjonssakData> saker = informasjonssakRepository.finnSakerMedMinsteInnvilgetOppholdperiodeInnenIntervall(batchArguments.getFom(), batchArguments.getTom());
        saker.forEach(sak -> {
            log.info("Oppretter informasjonssak-task for {}", sak.getAktørId().getId());
            ProsessTaskData data = new ProsessTaskData(OpprettInformasjonsFagsakTask.TASKTYPE);
            data.setAktørId(sak.getAktørId().getId());
            data.setCallIdFraEksisterende();
            data.setPrioritet(100);
            data.setProperty(OpprettInformasjonsFagsakTask.OPPRETTET_DATO_KEY, sak.getKildesakOpprettetDato().toString());
            data.setProperty(OpprettInformasjonsFagsakTask.FH_DATO_KEY, sak.getFamilieHndelseDato().toString());
            data.setProperty(OpprettInformasjonsFagsakTask.BEH_ENHET_ID_KEY, sak.getEnhet());
            data.setProperty(OpprettInformasjonsFagsakTask.BEH_ENHET_NAVN_KEY, sak.getEnhetNavn());
            data.setProperty(OpprettInformasjonsFagsakTask.BEHANDLING_AARSAK, BehandlingÅrsakType.INFOBREV_OPPHOLD.getKode());
            data.setProperty(OpprettInformasjonsFagsakTask.FAGSAK_ID_MOR_KEY, sak.getKildeFagsakId().toString());
            prosessTaskRepository.lagre(data);
        });
        return BATCHNAVN + "-" + saker.size();
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
