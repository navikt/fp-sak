package no.nav.foreldrepenger.domene.vedtak.intern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.sendVedtaksbrev", prioritet = 2)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SendVedtaksbrevTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(SendVedtaksbrevTask.class);

    private BehandlingVedtakRepository behandlingVedtakRepository;
    private SkalSendeVedtaksbrevUtleder skalSendeVedtaksbrevUtleder;
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;

    SendVedtaksbrevTask() {
        // for CDI proxy
    }

    @Inject
    public SendVedtaksbrevTask(BehandlingRepositoryProvider repositoryProvider,
                               SkalSendeVedtaksbrevUtleder skalSendeVedtaksbrevUtleder,
                               DokumentBestillerTjeneste dokumentBestillerTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.skalSendeVedtaksbrevUtleder = skalSendeVedtaksbrevUtleder;
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var behandlingVedtakOpt = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId);
        if (behandlingVedtakOpt.isEmpty()) {
            LOG.info("Det foreligger ikke vedtak i behandling: {}, kan ikke sende vedtaksbrev", behandlingId);
            return;
        }

        var vedtaksbrevStatus = skalSendeVedtaksbrevUtleder.statusVedtaksbrev(behandlingId);
        if (vedtaksbrevStatus.vedtaksbrevSkalProduseres()) {
            LOG.info("Sender vedtaksbrev for behandlingId: {}", behandlingId);
            dokumentBestillerTjeneste.produserVedtaksbrev(behandlingVedtakOpt.get());
        } else {
            LOG.info("Sender IKKE vedtaksbrev pga {} for behandling: {}", vedtaksbrevStatus, behandlingId);
        }
        LOG.info("Utført for behandling: {}", behandlingId);
    }
}
