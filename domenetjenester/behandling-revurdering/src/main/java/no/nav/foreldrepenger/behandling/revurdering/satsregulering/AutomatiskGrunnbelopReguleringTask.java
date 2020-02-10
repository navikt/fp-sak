package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.StartBehandlingTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

/**
 * @Dependent scope for å hente konfig ved hver kjøring.
 */
@Dependent
@ProsessTask(AutomatiskGrunnbelopReguleringTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AutomatiskGrunnbelopReguleringTask extends FagsakProsessTask {
    public static final String TASKTYPE = "behandlingsprosess.satsregulering";
    private static final Logger log = LoggerFactory.getLogger(AutomatiskGrunnbelopReguleringTask.class);
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private FagsakRepository fagsakRepository;

    AutomatiskGrunnbelopReguleringTask() {
        // for CDI proxy
    }

    @Inject
    public AutomatiskGrunnbelopReguleringTask(BehandlingRepositoryProvider repositoryProvider,
                                              ProsessTaskRepository prosessTaskRepository) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.prosessTaskRepository = prosessTaskRepository;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        final Long fagsakId = prosessTaskData.getFagsakId();
        boolean åpneYtelsesBehandlinger = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsakId).stream()
            .anyMatch(Behandling::erYtelseBehandling);

        if (åpneYtelsesBehandlinger) {
            return;
        }

        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        Behandling revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, BehandlingÅrsakType.RE_SATS_REGULERING, Optional.empty());

        log.info("GrunnbeløpRegulering har opprettet revurdering på fagsak med fagsakId = {}", fagsakId);

        ProsessTaskData fortsettTaskData = new ProsessTaskData(StartBehandlingTask.TASKTYPE);
        fortsettTaskData.setBehandling(revurdering.getFagsakId(), revurdering.getId(), revurdering.getAktørId().getId());
        fortsettTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(fortsettTaskData);
    }
}
