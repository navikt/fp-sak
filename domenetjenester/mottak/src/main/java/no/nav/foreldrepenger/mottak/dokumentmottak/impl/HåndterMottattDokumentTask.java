package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.DokumentPersistererTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(HåndterMottattDokumentTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class HåndterMottattDokumentTask extends FagsakProsessTask {

    public static final String TASKTYPE = "innhentsaksopplysninger.håndterMottattDokument";
    public static final String BEHANDLING_ÅRSAK_TYPE_KEY = "arsakType";
    public static final String MOTTATT_DOKUMENT_ID_KEY = "mottattDokumentId";

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(HåndterMottattDokumentTask.class);

    private InnhentDokumentTjeneste innhentDokumentTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private BehandlingRepository behandlingRepository;
    private DokumentPersistererTjeneste dokumentPersistererTjeneste;

    HåndterMottattDokumentTask() {
        // for CDI proxy
    }

    @Inject
    public HåndterMottattDokumentTask(InnhentDokumentTjeneste innhentDokumentTjeneste, DokumentPersistererTjeneste dokumentPersistererTjeneste,
                                      MottatteDokumentTjeneste mottatteDokumentTjeneste, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.innhentDokumentTjeneste = innhentDokumentTjeneste;
        this.dokumentPersistererTjeneste = dokumentPersistererTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        Long dokumentId = Long.valueOf(prosessTaskData.getPropertyValue(MOTTATT_DOKUMENT_ID_KEY));
        MottattDokument mottattDokument = mottatteDokumentTjeneste.hentMottattDokument(dokumentId)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: HåndterMottattDokument uten gyldig mottatt dokument, id=" + dokumentId.toString()));
        BehandlingÅrsakType behandlingÅrsakType = BehandlingÅrsakType.UDEFINERT;
        if (prosessTaskData.getPropertyValue(BEHANDLING_ÅRSAK_TYPE_KEY) != null) {
            behandlingÅrsakType = BehandlingÅrsakType.fraKode(prosessTaskData.getPropertyValue(BEHANDLING_ÅRSAK_TYPE_KEY));
        }
        log.info("HåndterMottattDokument taskId {} fagsakId {} behandlingId {} dokumentid {}", prosessTaskData.getId(), prosessTaskData.getFagsakId(), prosessTaskData.getBehandlingId(), mottattDokument.getId());
        if (behandlingId != null) {
            innhentDokumentTjeneste.opprettFraTidligereBehandling(behandlingId, mottattDokument, behandlingÅrsakType);
        } else {
            if (mottattDokument.getPayloadXml() != null) {
                dokumentPersistererTjeneste.xmlTilWrapper(mottattDokument);
            }
            innhentDokumentTjeneste.utfør(mottattDokument, behandlingÅrsakType);
        }
    }

    @Override
    protected List<Long> identifiserBehandling(ProsessTaskData prosessTaskData) {
        return behandlingRepository.hentÅpneBehandlingerIdForFagsakId(prosessTaskData.getFagsakId());
    }
}
