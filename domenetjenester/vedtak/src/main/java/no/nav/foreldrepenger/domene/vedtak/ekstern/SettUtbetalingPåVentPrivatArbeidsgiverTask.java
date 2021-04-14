package no.nav.foreldrepenger.domene.vedtak.ekstern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(SettUtbetalingPåVentPrivatArbeidsgiverTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SettUtbetalingPåVentPrivatArbeidsgiverTask extends GenerellProsessTask {
    private static final Logger LOG = LoggerFactory.getLogger(SettUtbetalingPåVentPrivatArbeidsgiverTask.class);
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
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            vurderOmSetteUtbetalingPåVentPrivatArbeidsgiver.opprettOppgave(behandling);
            LOG.info("SettUtbetalingPåVentPrivatArbeidsgiverTask: Vurderer for behandling: {}", behandlingId); //$NON-NLS-1$
        } else {
            LOG.info("SettUtbetalingPåVentPrivatArbeidsgiverTask: Ikke aktuelt for behandling: {}", behandlingId); //$NON-NLS-1$
        }

    }

}
