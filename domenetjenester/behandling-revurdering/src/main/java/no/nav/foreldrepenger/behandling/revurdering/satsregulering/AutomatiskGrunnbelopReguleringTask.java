package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(AutomatiskGrunnbelopReguleringTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AutomatiskGrunnbelopReguleringTask extends FagsakProsessTask {
    public static final String TASKTYPE = "behandlingsprosess.satsregulering";
    private static final Logger LOG = LoggerFactory.getLogger(AutomatiskGrunnbelopReguleringTask.class);
    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlendeEnhetTjeneste enhetTjeneste;
    private BehandlingFlytkontroll flytkontroll;

    AutomatiskGrunnbelopReguleringTask() {
        // for CDI proxy
    }

    @Inject
    public AutomatiskGrunnbelopReguleringTask(BehandlingRepositoryProvider repositoryProvider,
                                              BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
            BehandlendeEnhetTjeneste enhetTjeneste,
            BehandlingFlytkontroll flytkontroll) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.enhetTjeneste = enhetTjeneste;
        this.flytkontroll = flytkontroll;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        // Implisitt precondition fra utvalget i batches: Ingen ytelsesbehandlinger
        // utenom evt berørt behandling.
        var åpneYtelsesBehandlinger = behandlingRepository.harÅpenOrdinærYtelseBehandlingerForFagsakId(fagsakId);
        if (åpneYtelsesBehandlinger) {
            LOG.info("GrunnbeløpRegulering finnes allerede åpen revurdering på fagsakId = {}", fagsakId);
            return;
        }

        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        var skalKøes = flytkontroll.nyRevurderingSkalVente(fagsak);
        var enhet = enhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        var revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, BehandlingÅrsakType.RE_SATS_REGULERING, enhet);

        LOG.info("GrunnbeløpRegulering har opprettet revurdering på fagsak med fagsakId = {}", fagsakId);

        if (skalKøes) {
            flytkontroll.settNyRevurderingPåVent(revurdering);
        } else {
            behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
        }
    }
}
