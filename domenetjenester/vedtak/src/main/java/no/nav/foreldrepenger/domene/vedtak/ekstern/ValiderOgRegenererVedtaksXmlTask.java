package no.nav.foreldrepenger.domene.vedtak.ekstern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(ValiderOgRegenererVedtaksXmlTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class ValiderOgRegenererVedtaksXmlTask implements ProsessTaskHandler {


    public static final String TASKTYPE = "iverksetteVedtak.validerOgRegenererVedtaksXmlTask";

    private BehandlingRepository behandlingRepository;

    private RegenererVedtaksXmlTjeneste regenererVedtaksXmlTjeneste;

    public ValiderOgRegenererVedtaksXmlTask() {
    }

    @Inject
    public ValiderOgRegenererVedtaksXmlTask(RegenererVedtaksXmlTjeneste regenererVedtaksXmlTjeneste, BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
        this.regenererVedtaksXmlTjeneste = regenererVedtaksXmlTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long behandlingId = prosessTaskData.getBehandlingId();
        Behandling behandling = finnBehandling(behandlingId);
        regenererVedtaksXmlTjeneste.validerOgRegenerer(behandling);

    }

    protected Behandling finnBehandling(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId);
    }


}
