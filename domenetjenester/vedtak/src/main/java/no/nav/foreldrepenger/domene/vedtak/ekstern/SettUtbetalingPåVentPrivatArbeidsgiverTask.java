package no.nav.foreldrepenger.domene.vedtak.ekstern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(SettUtbetalingPåVentPrivatArbeidsgiverTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SettUtbetalingPåVentPrivatArbeidsgiverTask extends BehandlingProsessTask {
    private static final Logger log = LoggerFactory.getLogger(SettUtbetalingPåVentPrivatArbeidsgiverTask.class);
    public static final String TASKTYPE = "iverksetteVedtak.oppgaveUtbetalingPåVent";

    private BehandlingRepository behandlingRepository;
    private VurderOmSetteUtbetalingPåVentPrivatArbeidsgiver vurderOmSetteUtbetalingPåVentPrivatArbeidsgiver;

    public SettUtbetalingPåVentPrivatArbeidsgiverTask() {
        // CDI
    }

    @Inject
    public SettUtbetalingPåVentPrivatArbeidsgiverTask(BehandlingRepositoryProvider repositoryProvider,
                                                      BehandlingRepository behandlingRepository,
                                                      VurderOmSetteUtbetalingPåVentPrivatArbeidsgiver vurderOmSetteUtbetalingPåVentPrivatArbeidsgiver) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = behandlingRepository;
        this.vurderOmSetteUtbetalingPåVentPrivatArbeidsgiver = vurderOmSetteUtbetalingPåVentPrivatArbeidsgiver;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Long behandlingId = prosessTaskData.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.getFagsakYtelseType().gjelderForeldrepenger()) {
            vurderOmSetteUtbetalingPåVentPrivatArbeidsgiver.opprettOppgave(behandling);
            log.info("SettUtbetalingPåVentPrivatArbeidsgiverTask: Vurderer for behandling: {}", behandlingId); //$NON-NLS-1$
        } else {
            log.info("SettUtbetalingPåVentPrivatArbeidsgiverTask: Ikke aktuelt for behandling: {}", behandlingId); //$NON-NLS-1$
        }

    }
}
