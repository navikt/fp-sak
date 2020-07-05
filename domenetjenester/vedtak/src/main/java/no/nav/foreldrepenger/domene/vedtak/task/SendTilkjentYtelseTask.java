package no.nav.foreldrepenger.domene.vedtak.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(SendTilkjentYtelseTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SendTilkjentYtelseTask extends GenerellProsessTask {
    public static final String TASKTYPE = "iverksetteVedtak.sendTilkjentYtelse";
    private TilkjentYtelseMeldingProducer meldingProducer;
    private BehandlingRepository behandlingRepository;

    public SendTilkjentYtelseTask() {
        // CDI krav
    }

    @Inject
    public SendTilkjentYtelseTask(TilkjentYtelseMeldingProducer meldingProducer, BehandlingRepository behandlingRepository) {
        super();
        this.meldingProducer = meldingProducer;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        meldingProducer.sendTilkjentYtelse(behandling);
    }

}
