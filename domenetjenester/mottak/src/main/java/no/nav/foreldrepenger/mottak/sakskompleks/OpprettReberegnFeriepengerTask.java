package no.nav.foreldrepenger.mottak.sakskompleks;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.BerørtBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.StartBehandlingTask;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@ProsessTask(OpprettReberegnFeriepengerTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettReberegnFeriepengerTask extends FagsakProsessTask {
    public static final String TASKTYPE = "oppdater.yf.soknad.mottattdato";
    private static final Logger LOG = LoggerFactory.getLogger(OpprettReberegnFeriepengerTask.class);
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private FagsakRepository fagsakRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private BehandlendeEnhetTjeneste enhetTjeneste;
    private BerørtBehandlingTjeneste berørtBehandlingTjeneste;

    OpprettReberegnFeriepengerTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettReberegnFeriepengerTask(BehandlingRepositoryProvider repositoryProvider,
                                          ProsessTaskRepository prosessTaskRepository,
                                          BehandlendeEnhetTjeneste enhetTjeneste,
                                          BerørtBehandlingTjeneste berørtBehandlingTjeneste) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.prosessTaskRepository = prosessTaskRepository;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.enhetTjeneste = enhetTjeneste;
        this.berørtBehandlingTjeneste = berørtBehandlingTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        // Implisitt precondition fra utvalget i batches: Ingen ytelsesbehandlinger
        // utenom evt berørt behandling.
        boolean åpneYtelsesBehandlinger = behandlingRepository.harÅpenOrdinærYtelseBehandlingerForFagsakId(fagsakId);
        if (åpneYtelsesBehandlinger) {
            LOG.info("FERIEREBEREGN finnes åpen revurdering på fagsakId = {}", fagsakId);
            return;
        }
        var åpenBehandlingMedforelder = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsakId)
            .flatMap(fr -> fr.getRelatertFagsakFraId(fagsakId))
            .map(f -> behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(f.getId()).stream()
                    .anyMatch(b -> b.erRevurdering() && !b.harBehandlingÅrsak(BehandlingÅrsakType.REBEREGN_FERIEPENGER)))
            .orElse(false);
        if (åpenBehandlingMedforelder) {
            LOG.info("FERIEREBEREGN finnes åpen revurdering på 2part fagsakId = {}", fagsakId);
            return;
        }

        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        var enhet = enhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        Behandling revurdering = revurderingTjeneste.opprettAutomatiskRevurderingMultiÅrsak(fagsak,
            List.of(BehandlingÅrsakType.BERØRT_BEHANDLING, BehandlingÅrsakType.REBEREGN_FERIEPENGER), enhet);
        berørtBehandlingTjeneste.opprettHistorikkinnslagOmRevurdering(revurdering, BehandlingÅrsakType.BERØRT_BEHANDLING);
        LOG.info("FERIEREBEREGN har opprettet revurdering på fagsak {} med behandlingId = {}", fagsak.getSaksnummer(), revurdering.getId());

        ProsessTaskData fortsettTaskData = new ProsessTaskData(StartBehandlingTask.TASKTYPE);
        fortsettTaskData.setBehandling(revurdering.getFagsakId(), revurdering.getId(), revurdering.getAktørId().getId());
        fortsettTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(fortsettTaskData);

    }
}
