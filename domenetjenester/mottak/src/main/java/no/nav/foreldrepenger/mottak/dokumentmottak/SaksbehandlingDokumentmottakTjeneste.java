package no.nav.foreldrepenger.mottak.dokumentmottak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.HåndterMottattDokumentTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SaksbehandlingDokumentmottakTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SaksbehandlingDokumentmottakTjeneste.class);

    private ProsessTaskTjeneste taskTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;

    public SaksbehandlingDokumentmottakTjeneste() {
        //for CDI, jaja
    }

    @Inject
    public SaksbehandlingDokumentmottakTjeneste(ProsessTaskTjeneste taskTjeneste,
                                                MottatteDokumentTjeneste mottatteDokumentTjeneste) {
        this.taskTjeneste = taskTjeneste;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
    }

    public void dokumentAnkommet(MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {

        var mottattDokumentId = mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(mottattDokument);

        var prosessTaskData = ProsessTaskData.forProsessTask(HåndterMottattDokumentTask.class);
        prosessTaskData.setFagsakId(mottattDokument.getFagsakId());
        prosessTaskData.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, mottattDokumentId.toString());
        settÅrsakHvisDefinert(behandlingÅrsakType, prosessTaskData);
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
    }

    public void opprettFraTidligereBehandling(MottattDokument mottattDokument, Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        LOG.info("Oppretter håndtermottattdokumenttask fra tidligere behandling {} fagsak {} dokument {}", behandling.getId(), behandling.getFagsakId(), mottattDokument.getId());
        var prosessTaskData = ProsessTaskData.forProsessTask(HåndterMottattDokumentTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, mottattDokument.getId().toString());
        settÅrsakHvisDefinert(behandlingÅrsakType, prosessTaskData);
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
    }

    public void mottaUbehandletSøknad(MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {
        var prosessTaskData = ProsessTaskData.forProsessTask(HåndterMottattDokumentTask.class);

        prosessTaskData.setFagsakId(mottattDokument.getFagsakId());
        prosessTaskData.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, mottattDokument.getId().toString());
        settÅrsakHvisDefinert(behandlingÅrsakType, prosessTaskData);
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
    }

    private void settÅrsakHvisDefinert(BehandlingÅrsakType behandlingÅrsakType, ProsessTaskData prosessTaskData) {
        if (behandlingÅrsakType !=null && !BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType)) {
            prosessTaskData.setProperty(HåndterMottattDokumentTask.BEHANDLING_ÅRSAK_TYPE_KEY, behandlingÅrsakType.getKode());
        }
    }
}
