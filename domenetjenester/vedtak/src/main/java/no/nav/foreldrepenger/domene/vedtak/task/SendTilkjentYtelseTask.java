package no.nav.foreldrepenger.domene.vedtak.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(SendTilkjentYtelseTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SendTilkjentYtelseTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "iverksetteVedtak.sendTilkjentYtelse";
    private TilkjentYtelseMeldingProducer meldingProducer;
    private BehandlingRepository behandlingRepository;

    public SendTilkjentYtelseTask() {
        // CDI krav
    }

    @Inject
    public SendTilkjentYtelseTask(TilkjentYtelseMeldingProducer meldingProducer, BehandlingRepository behandlingRepository) {
        this.meldingProducer = meldingProducer;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long behandlingId = prosessTaskData.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        meldingProducer.sendTilkjentYtelse(behandling);
    }

}
