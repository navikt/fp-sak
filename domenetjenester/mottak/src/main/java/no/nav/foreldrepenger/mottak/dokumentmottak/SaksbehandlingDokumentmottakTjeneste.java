package no.nav.foreldrepenger.mottak.dokumentmottak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.HåndterMottattDokumentTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class SaksbehandlingDokumentmottakTjeneste {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(SaksbehandlingDokumentmottakTjeneste.class);

    private ProsessTaskRepository prosessTaskRepository;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;

    public SaksbehandlingDokumentmottakTjeneste() {
        //for CDI, jaja
    }

    @Inject
    public SaksbehandlingDokumentmottakTjeneste(ProsessTaskRepository prosessTaskRepository,
                                                MottatteDokumentTjeneste mottatteDokumentTjeneste) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
    }

    public void dokumentAnkommet(MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {

        var mottattDokumentId = mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(mottattDokument);

        var prosessTaskData = new ProsessTaskData(HåndterMottattDokumentTask.TASKTYPE);
        prosessTaskData.setFagsakId(mottattDokument.getFagsakId());
        prosessTaskData.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, mottattDokumentId.toString());
        settÅrsakHvisDefinert(behandlingÅrsakType, prosessTaskData);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    public void opprettFraTidligereBehandling(MottattDokument mottattDokument, Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        LOG.info("Oppretter håndtermottattdokumenttask fra tidligere behandling {} fagsak {} dokument {}", behandling.getId(), behandling.getFagsakId(), mottattDokument.getId());
        var prosessTaskData = new ProsessTaskData(HåndterMottattDokumentTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, mottattDokument.getId().toString());
        settÅrsakHvisDefinert(behandlingÅrsakType, prosessTaskData);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    public void mottaUbehandletSøknad(MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {
        var prosessTaskData = new ProsessTaskData(HåndterMottattDokumentTask.TASKTYPE);

        prosessTaskData.setFagsakId(mottattDokument.getFagsakId());
        prosessTaskData.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, mottattDokument.getId().toString());
        settÅrsakHvisDefinert(behandlingÅrsakType, prosessTaskData);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    private void settÅrsakHvisDefinert(BehandlingÅrsakType behandlingÅrsakType, ProsessTaskData prosessTaskData) {
        if (behandlingÅrsakType !=null && !BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType)) {
            prosessTaskData.setProperty(HåndterMottattDokumentTask.BEHANDLING_ÅRSAK_TYPE_KEY, behandlingÅrsakType.getKode());
        }
    }
}
