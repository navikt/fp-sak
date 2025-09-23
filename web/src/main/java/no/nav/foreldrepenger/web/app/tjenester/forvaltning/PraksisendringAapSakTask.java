package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "aap.praksisendring.sak", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class PraksisendringAapSakTask extends FagsakProsessTask {
    private static final Logger LOG = LoggerFactory.getLogger(PraksisendringAapSakTask.class);
    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private RevurderingTjeneste revurderingTjeneste;
    private FagsakRepository fagsakRepository;

    public PraksisendringAapSakTask() {
        // For CDI
    }

    @Inject
    public PraksisendringAapSakTask(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                    BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                    @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) RevurderingTjeneste revurderingTjeneste) {
        super(behandlingRepositoryProvider.getFagsakLåsRepository(), behandlingRepositoryProvider.getBehandlingLåsRepository());
        this.fagsakRepository = behandlingRepositoryProvider.getFagsakRepository();
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.revurderingTjeneste = revurderingTjeneste;
    }


    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId) {
        var saksnummer = prosessTaskData.getSaksnummer();
        LOG.info("AAP_PRAKSISENDRING_SAK_TASK: Starter task revurder sak praksisendring beregning av AAP");
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElseThrow();
        if (behandling.erAvsluttet()) {
            LOG.info("AAP_PRAKSISENDRING_SAK_TASK: fagsakId {} saksnummer {} Siste behandling er avsluttet, oppretter revurdering", fagsakId, saksnummer);
            var revurdering = opprettRevurdering(prosessTaskData, saksnummer);
            behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
        } else if (behandling.erRevurdering()) {
            LOG.info("AAP_PRAKSISENDRING_SAK_TASK: fagsakId {} saksnummer {} Siste behandling er åpen spesialbehandling, oppretter køet revurdering", fagsakId, saksnummer);
            var revurdering = opprettRevurdering(prosessTaskData, saksnummer);
            behandlingProsesseringTjeneste.enkøBehandling(revurdering);
        } else {
            LOG.info("AAP_PRAKSISENDRING_SAK_TASK: fagsakId {} saksnummer {} Siste behandling er åpen førstegangsbehdling, gjør ingenting.", fagsakId, saksnummer);
        }
    }

    private Behandling opprettRevurdering(ProsessTaskData prosessTaskData, String saksnummer) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(prosessTaskData.getSaksnummer())).orElseThrow();
        var revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, BehandlingÅrsakType.FEIL_PRAKSIS_BG_AAP_KOMBI, BehandlendeEnhetTjeneste.getMidlertidigEnhet());
        LOG.info("AAP_PRAKSISENDRING_SAK_TASK: fagsakId {} saksnummer {} Opprettet revurdering med uuid {}", fagsak.getId(), saksnummer, revurdering.getUuid());
        return revurdering;
    }

}
