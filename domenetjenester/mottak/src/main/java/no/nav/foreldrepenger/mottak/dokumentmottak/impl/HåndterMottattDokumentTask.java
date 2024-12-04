package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.MottattDokumentPersisterer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("innhentsaksopplysninger.håndterMottattDokument")
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class HåndterMottattDokumentTask extends FagsakProsessTask {

    public static final String BEHANDLING_ÅRSAK_TYPE_KEY = "arsakType";
    public static final String MOTTATT_DOKUMENT_ID_KEY = "mottattDokumentId";

    private static final Logger LOG = LoggerFactory.getLogger(HåndterMottattDokumentTask.class);

    private InnhentDokumentTjeneste innhentDokumentTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private BehandlingRepository behandlingRepository;
    private MottattDokumentPersisterer mottattDokumentPersisterer;

    HåndterMottattDokumentTask() {
        // for CDI proxy
    }

    @Inject
    public HåndterMottattDokumentTask(InnhentDokumentTjeneste innhentDokumentTjeneste, MottattDokumentPersisterer mottattDokumentPersisterer,
                                      MottatteDokumentTjeneste mottatteDokumentTjeneste, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.innhentDokumentTjeneste = innhentDokumentTjeneste;
        this.mottattDokumentPersisterer = mottattDokumentPersisterer;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var dokumentId = Long.valueOf(prosessTaskData.getPropertyValue(MOTTATT_DOKUMENT_ID_KEY));
        var mottattDokument = mottatteDokumentTjeneste.hentMottattDokument(dokumentId)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: HåndterMottattDokument uten gyldig mottatt dokument, id=" + dokumentId.toString()));
        var behandlingÅrsakType = Optional.ofNullable(prosessTaskData.getPropertyValue(BEHANDLING_ÅRSAK_TYPE_KEY))
            .map(BehandlingÅrsakType::fraKode).orElse(BehandlingÅrsakType.UDEFINERT);
        LOG.info("HåndterMottattDokument taskId {} saksnummer {} behandlingId {} dokumentid {}", prosessTaskData.getId(), prosessTaskData.getSaksnummer(), prosessTaskData.getBehandlingId(), mottattDokument.getId());
        if (behandlingId != null) {
            innhentDokumentTjeneste.opprettFraTidligereBehandling(behandlingId, mottattDokument, behandlingÅrsakType);
        } else {
            if (mottattDokument.getPayloadXml() != null) {
                mottattDokumentPersisterer.xmlTilWrapper(mottattDokument);
            }
            innhentDokumentTjeneste.utfør(mottattDokument, behandlingÅrsakType);
        }
    }

    @Override
    protected List<Long> identifiserBehandling(ProsessTaskData prosessTaskData) {
        return behandlingRepository.hentÅpneBehandlingerIdForFagsakId(prosessTaskData.getFagsakId());
    }
}
