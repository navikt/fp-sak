package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
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

    @Inject
    public PraksisendringAapSakTask(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                    BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                    RevurderingTjeneste revurderingTjeneste) {
        super(behandlingRepositoryProvider.getFagsakLåsRepository(), behandlingRepositoryProvider.getBehandlingLåsRepository());
        this.fagsakRepository = behandlingRepositoryProvider.getFagsakRepository();
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.revurderingTjeneste = revurderingTjeneste;
    }


    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId) {
        LOG.info("Starter task revurder sak praksisendring beregning av AAP");
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElseThrow();
        if (behandling.erAvsluttet()) {
            var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(prosessTaskData.getSaksnummer())).orElseThrow();
            var revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, BehandlingÅrsakType.FEIL_PRAKSIS_BEREGNING_AAP_KOMBINASJON, null); // TODO Fiks enhet
            behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
        } else {
            // Saker som startet etter beregning og ble opprettet før endringsdato, men har blitt liggende
            if (behandling.getStartpunkt().equals(StartpunktType.TILKJENT_YTELSE) || behandling.getStartpunkt().equals(StartpunktType.UTTAKSVILKÅR)) {
                var lås = behandlingRepository.taSkriveLås(behandling);
                if (behandling.isBehandlingPåVent()) {
                    behandlingProsesseringTjeneste.taBehandlingAvVent(behandling);
                }
                behandlingProsesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling, lås, BehandlingStegType.DEKNINGSGRAD);
                behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
                // TODO Legge på behandlingårsaktype?
                // TODO Flytte til egen enhet?
            }
        }

    }

}
