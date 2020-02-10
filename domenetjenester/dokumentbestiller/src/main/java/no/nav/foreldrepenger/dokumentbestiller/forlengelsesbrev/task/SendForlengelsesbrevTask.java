package no.nav.foreldrepenger.dokumentbestiller.forlengelsesbrev.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.dokumentbestiller.forlengelsesbrev.SendForlengelsesbrevTaskProperties;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.util.FPDateUtil;

@ApplicationScoped
@ProsessTask(SendForlengelsesbrevTaskProperties.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SendForlengelsesbrevTask implements ProsessTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(SendForlengelsesbrevTask.class);

    private BehandlingRepository behandlingRepository;

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    SendForlengelsesbrevTask() {
        // for CDI proxy
    }

    @Inject
    public SendForlengelsesbrevTask(BehandlingRepository behandlingRepository,
                                    BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long behandlingId = prosessTaskData.getBehandlingId();
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandlingsfristUtløpt(behandling)) {
            sendForlengelsesbrevOgOppdaterBehandling(behandling, kontekst);
            log.info("Utført for behandling: {}", behandlingId);
        } else {
            log.info("Ikke utført for behandling: {}, behandlingsfrist ikke utløpt", behandlingId);
        }
    }

    private void sendForlengelsesbrevOgOppdaterBehandling(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        behandling.setBehandlingstidFrist(FPDateUtil.iDag().plusWeeks(behandling.getType().getBehandlingstidFristUker()));
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        if (!skalSendeForlengelsesbrevAutomatisk(behandling.getId())) { //NOSONAR
            return;
        }
        // Kommentert ut siden ForlengetDokument er markert for fjerning fra fpsak. Hvis brevet noen gang skal taes i bruk
        // igjen, må det bestilles via fpformidling istedet.
/*      DokumentType forlengetDokument = new ForlengetDokument(DokumentMalType.FORLENGET_DOK);
        Long dokumentDataId = dokumentDataTjeneste.lagreDokumentData(behandling.getId(), forlengetDokument);
        dokumentBestillerApplikasjonTjeneste.produserDokument(dokumentDataId, HistorikkAktør.VEDTAKSLØSNINGEN, null);*/
    }

    private boolean skalSendeForlengelsesbrevAutomatisk(Long behandlingId) {
        log.info("Brev ikke sendt for behandling: {}, automatisk utsendelse slått av for alle", behandlingId);
        return false;
    }

    private boolean behandlingsfristUtløpt(Behandling behandling) {
        return FPDateUtil.iDag().isAfter(behandling.getBehandlingstidFrist());
    }
}
