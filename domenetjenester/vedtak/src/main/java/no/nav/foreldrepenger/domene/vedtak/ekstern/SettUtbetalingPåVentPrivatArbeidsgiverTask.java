package no.nav.foreldrepenger.domene.vedtak.ekstern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(SettUtbetalingPåVentPrivatArbeidsgiverTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SettUtbetalingPåVentPrivatArbeidsgiverTask extends GenerellProsessTask {
    private static final Logger log = LoggerFactory.getLogger(SettUtbetalingPåVentPrivatArbeidsgiverTask.class);
    public static final String TASKTYPE = "iverksetteVedtak.oppgaveUtbetalingPåVent";

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
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.getFagsakYtelseType().gjelderForeldrepenger()) {
            vurderOmSetteUtbetalingPåVentPrivatArbeidsgiver.opprettOppgave(behandling);
            log.info("SettUtbetalingPåVentPrivatArbeidsgiverTask: Vurderer for behandling: {}", behandlingId); //$NON-NLS-1$
        } else {
            log.info("SettUtbetalingPåVentPrivatArbeidsgiverTask: Ikke aktuelt for behandling: {}", behandlingId); //$NON-NLS-1$
        }

    }

}
