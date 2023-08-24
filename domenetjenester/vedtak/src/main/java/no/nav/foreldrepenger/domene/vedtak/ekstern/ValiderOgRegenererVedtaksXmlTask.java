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
@ProsessTask(value = "iverksetteVedtak.validerOgRegenererVedtaksXmlTask", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class ValiderOgRegenererVedtaksXmlTask extends GenerellProsessTask {


    private BehandlingRepository behandlingRepository;

    private RegenererVedtaksXmlTjeneste regenererVedtaksXmlTjeneste;

    public ValiderOgRegenererVedtaksXmlTask() {
    }

    @Inject
    public ValiderOgRegenererVedtaksXmlTask(RegenererVedtaksXmlTjeneste regenererVedtaksXmlTjeneste, BehandlingRepository behandlingRepository) {
        super();
        this.behandlingRepository = behandlingRepository;
        this.regenererVedtaksXmlTjeneste = regenererVedtaksXmlTjeneste;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = finnBehandling(behandlingId);
        regenererVedtaksXmlTjeneste.validerOgRegenerer(behandling);

    }

    protected Behandling finnBehandling(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId);
    }


}

