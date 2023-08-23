package no.nav.foreldrepenger.domene.vedtak.ekstern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.oppgaveUtbetalingPåVent", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SettUtbetalingPåVentPrivatArbeidsgiverTask extends GenerellProsessTask {


    private BehandlingRepository behandlingRepository;
    private VurderOmSetteUtbetalingPåVentPrivatArbeidsgiver vurderOmSetteUtbetalingPåVentPrivatArbeidsgiver;

    public SettUtbetalingPåVentPrivatArbeidsgiverTask() {
        // CDI
    }

    @Inject
    public SettUtbetalingPåVentPrivatArbeidsgiverTask(BehandlingRepository behandlingRepository,
                                                      VurderOmSetteUtbetalingPåVentPrivatArbeidsgiver vurderOmSetteUtbetalingPåVentPrivatArbeidsgiver) {
        super();
        this.behandlingRepository = behandlingRepository;
        this.vurderOmSetteUtbetalingPåVentPrivatArbeidsgiver = vurderOmSetteUtbetalingPåVentPrivatArbeidsgiver;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            vurderOmSetteUtbetalingPåVentPrivatArbeidsgiver.opprettOppgave(behandling);
        }
    }

}
