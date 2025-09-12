package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
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
    private static final LocalDate DATO_PRAKSISENDRING = LocalDate.of(2025,9,1);
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
        LOG.info("aap.praksisendring.sak: Starter task revurder sak praksisendring beregning av AAP");
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElseThrow();
        if (behandling.erAvsluttet()) {
            LOG.info("aap.praksisendring.sak: fagsakId {} Siste behandling er avsluttet, oppretter revurdering", fagsakId);
            var revurdering = opprettRevurdering(prosessTaskData, fagsakId);
            behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
        } else if (SpesialBehandling.erSpesialBehandling(behandling)) {
            LOG.info("aap.praksisendring.sak: fagsakId {} Siste behandling er åpen spesialbehandling, oppretter køet revurdering", fagsakId);
            var revurdering = opprettRevurdering(prosessTaskData, fagsakId);
            behandlingProsesseringTjeneste.enkøBehandling(revurdering);
        } else if (behandling.getType().equals(BehandlingType.FØRSTEGANGSSØKNAD)) {
            LOG.info("aap.praksisendring.sak: fagsakId {} Siste behandling er åpen førstegangsbehdling, gjør ingenting.", fagsakId);
        } else {
            if (harIkkeKjørtNyeBeregningsregler(behandling)) {
                LOG.info("aap.praksisendring.sak: fagsakId {} Siste er åpen revurdering med uuid {} som er passert beregning, ruller den tilbake", fagsakId, behandling.getUuid());
                var lås = behandlingRepository.taSkriveLås(behandling);
                if (behandling.isBehandlingPåVent()) {
                    behandlingProsesseringTjeneste.taBehandlingAvVent(behandling);
                }
                behandlingProsesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling, lås, BehandlingStegType.DEKNINGSGRAD);
                behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
            }
        }

    }

    private boolean harIkkeKjørtNyeBeregningsregler(Behandling behandling) {
        return behandling.getStartpunkt().equals(StartpunktType.UTTAKSVILKÅR) || (
            behandling.getOpprettetDato().toLocalDate().isBefore(DATO_PRAKSISENDRING) && behandlingProsesseringTjeneste.erBehandlingEtterSteg(
                behandling, BehandlingStegType.DEKNINGSGRAD));
    }

    private Behandling opprettRevurdering(ProsessTaskData prosessTaskData, Long fagsakId) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(prosessTaskData.getSaksnummer())).orElseThrow();
        var revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, BehandlingÅrsakType.FEIL_PRAKSIS_BEREGNING_AAP_KOMBINASJON, BehandlendeEnhetTjeneste.getMidlertidigEnhet());
        LOG.info("aap.praksisendring.sak: fagsakId {} Opprettet revurdering med uuid {}", fagsakId, revurdering.getUuid());
        return revurdering;
    }

}
