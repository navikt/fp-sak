package no.nav.foreldrepenger.produksjonsstyring.behandlinghendelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "behandling.avslutt.ekstern", prioritet = 2)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class AvsluttEksterneGrunnlagTask extends GenerellProsessTask {
    private static final Logger LOG = LoggerFactory.getLogger(AvsluttEksterneGrunnlagTask.class);

    private BeregningTjeneste beregningTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private BehandlingRepository behandlingRepository;

    public AvsluttEksterneGrunnlagTask() {
        // CDI
    }

    @Inject
    public AvsluttEksterneGrunnlagTask(BeregningTjeneste beregningTjeneste,
                                            InntektArbeidYtelseTjeneste iayTjeneste,
                                            BehandlingRepository behandlingRepository) {
        this.beregningTjeneste = beregningTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);
        if (!ref.fagsakYtelseType().equals(FagsakYtelseType.ENGANGSTØNAD) && ref.behandlingType().erYtelseBehandlingType()) {
            LOG.info("Avslutter behandling {} i abakus og kalkulus", ref.behandlingUuid());
            iayTjeneste.avslutt(ref.behandlingId());
            beregningTjeneste.avslutt(ref);
        }
    }
}
