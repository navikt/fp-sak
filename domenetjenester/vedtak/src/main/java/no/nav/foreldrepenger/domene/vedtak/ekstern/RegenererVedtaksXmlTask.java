package no.nav.foreldrepenger.domene.vedtak.ekstern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(RegenererVedtaksXmlTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class RegenererVedtaksXmlTask extends GenerellProsessTask {


    public static final String TASKTYPE = "iverksetteVedtak.regenererVedtaksXmlTask";

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
        Behandling behandling = finnBehandling(behandlingId);
        regenererVedtaksXmlTjeneste.regenerer(behandling);

    }

    protected Behandling finnBehandling(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId);
    }
}
