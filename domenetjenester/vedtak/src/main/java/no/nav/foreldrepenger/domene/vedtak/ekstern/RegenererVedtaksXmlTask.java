package no.nav.foreldrepenger.domene.vedtak.ekstern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.regenererVedtaksXmlTask" , maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class RegenererVedtaksXmlTask extends GenerellProsessTask {


    private BehandlingRepository behandlingRepository;

    private RegenererVedtaksXmlTjeneste regenererVedtaksXmlTjeneste;

    public RegenererVedtaksXmlTask() {
    }

    @Inject
    public RegenererVedtaksXmlTask(RegenererVedtaksXmlTjeneste regenererVedtaksXmlTjeneste, BehandlingRepository behandlingRepository) {
        super();
        this.behandlingRepository = behandlingRepository;
        this.regenererVedtaksXmlTjeneste = regenererVedtaksXmlTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = finnBehandling(behandlingId);
        regenererVedtaksXmlTjeneste.regenerer(behandling);

    }

    protected Behandling finnBehandling(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId);
    }
}
