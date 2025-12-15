package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.util.Collections;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "migrering.tilbakeforbehandling.refusjonstilfelle", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class TilbakeføringBeregningRefusjonstilfelleTask extends BehandlingProsessTask {
    private static final Logger LOG = LoggerFactory.getLogger(TilbakeføringBeregningRefusjonstilfelleTask.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste prosesseringTjeneste;
    private BeregningTjeneste beregningTjeneste;

    public TilbakeføringBeregningRefusjonstilfelleTask() {
        // For CDI
    }

    @Inject
    public TilbakeføringBeregningRefusjonstilfelleTask(BehandlingRepositoryProvider repositoryProvider,
                                                       BehandlingProsesseringTjeneste prosesseringTjeneste, BeregningTjeneste beregningTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.prosesseringTjeneste = prosesseringTjeneste;
        this.beregningTjeneste = beregningTjeneste;
    }



    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var harRefusjonstilfelle = harRefusjonstilfelle(behandling);
        if (!harRefusjonstilfelle) {
            LOG.info("Behandling {} med saksnummer {} har ikke refusjonstilfelle, ingen tilbakeføring nødvendig.", behandling.getId(), behandling.getSaksnummer());
        } else {
            LOG.info("Behandling {} med saksnummer {} har refusjonstilfelle, tilbakefører behandling.", behandling.getId(), behandling.getSaksnummer());
            taAvVentOgTilbakefør(behandling);
        }
    }

    private void taAvVentOgTilbakefør(Behandling behandling) {
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        var stegFørBeregning = behandling.getFagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER) ? BehandlingStegType.DEKNINGSGRAD : BehandlingStegType.VURDER_SAMLET;
        if (behandling.isBehandlingPåVent()) {
            prosesseringTjeneste.taBehandlingAvVent(behandling);
        }
        prosesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling, lås, stegFørBeregning);
        prosesseringTjeneste.opprettTasksForFortsettBehandling(behandling);

    }

    private boolean harRefusjonstilfelle(Behandling behandling) {
        var grunnlag = beregningTjeneste.hent(BehandlingReferanse.fra(behandling));
        return grunnlag.flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag)
            .map(Beregningsgrunnlag::getFaktaOmBeregningTilfeller)
            .orElse(Collections.emptyList())
            .stream()
            .anyMatch(FaktaOmBeregningTilfelle.VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT::equals);

    }
}
