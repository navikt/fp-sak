package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

@Dependent
@ProsessTask(value = "tilbakehopp.feilretting.batch", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class BeregningTilbakehoppFeilretterBatchTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(BeregningTilbakehoppFeilretterBatchTask.class);
    private static final String FRA_OG_MED = "fraOgMed";
    private static final String TIL_OG_MED = "tilOgMed";
    private static final String DRY_RUN = "dryRun";

    private final BehandlingProsesseringTjeneste prosesseringTjeneste;
    private final EntityManager entityManager;
    private final ProsessTaskTjeneste prosessTaskTjeneste;
    private final BehandlingRepository behandlingRepository;
    private final HistorikkinnslagRepository historikkinnslagRepository;

    @Inject
    public BeregningTilbakehoppFeilretterBatchTask(BehandlingProsesseringTjeneste prosesseringTjeneste,
                                                   EntityManager entityManager,
                                                   ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosesseringTjeneste = prosesseringTjeneste;
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.behandlingRepository = new BehandlingRepository(entityManager);
        this.historikkinnslagRepository = new HistorikkinnslagRepository(entityManager);
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fraOgMedId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_OG_MED)).map(Long::valueOf).orElseThrow();
        var tilOgMedId = Optional.ofNullable(prosessTaskData.getPropertyValue(TIL_OG_MED)).map(Long::valueOf).orElseThrow();
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).filter("false"::equalsIgnoreCase).isEmpty();

        var behandlinger = finnNesteTiSaker(fraOgMedId, tilOgMedId).toList();

        if (dryRun) {
            LOG.info("Fant {} behandlinger som kan rulles tilbake, men gjøres ikke i dryRun", behandlinger.size());
        } else {
            LOG.info("Fant {} behandlinger som kan rulles tilbake", behandlinger.size());
            behandlinger.forEach(this::rullTilbakeBehandling);
        }

        behandlinger.stream()
            .map(Behandling::getId)
            .max(Long::compareTo)
            .ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettNesteTask(nesteId+1, dryRun, tilOgMedId)));
    }

    private void rullTilbakeBehandling(Behandling behandling) {
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        if (behandling.isBehandlingPåVent()) {
            prosesseringTjeneste.taBehandlingAvVent(behandling);
        }
        var tilSteg = finnStegÅHoppeTil(behandling);
        lagHistorikkinnslag(behandling, tilSteg.getNavn());
        prosesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling, lås, tilSteg);
        prosesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, LocalDateTime.now());
    }

    private BehandlingStegType finnStegÅHoppeTil(Behandling behandling) {
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
            // SVP har ikke dekningsgradsteg
            return BehandlingStegType.VURDER_SAMLET;
        }
        // Ved start i uttak / tilkjent må vi kopiere beregningsgrunnlaget fra originalbehandling i KOFAK steget
        var harStartPunktEtterBeregning = StartpunktType.UTTAKSVILKÅR.equals(behandling.getStartpunkt())
            || StartpunktType.TILKJENT_YTELSE.equals(behandling.getStartpunkt());
        return harStartPunktEtterBeregning ? BehandlingStegType.KONTROLLER_FAKTA : BehandlingStegType.DEKNINGSGRAD;
    }


    private void lagHistorikkinnslag(Behandling behandling, String tilStegNavn) {
        var fraStegNavn = behandling.getAktivtBehandlingSteg() != null ? behandling.getAktivtBehandlingSteg().getNavn() : null;
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel("Behandlingen er flyttet")
            .addLinje(String.format("Behandlingen er flyttet fra __%s__ tilbake til __%s__", fraStegNavn, tilStegNavn))
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private Stream<Behandling> finnNesteTiSaker(Long fraOgMedId, Long tilOgMedId) {
        var sql = """
              select * from (
                select *
                from behandling
                where id in (
            select distinct(beh.id) from FPSAK.BEHANDLING beh
            inner join FPSAK.GR_BEREGNINGSGRUNNLAG gr on gr.BEHANDLING_ID = beh.id
            inner join FPSAK.BG_EKSTERN_KOBLING kob on kob.BEHANDLING_ID = beh.id
            where gr.AKTIV = 'J' and gr.OPPRETTET_TID > kob.OPPRETTET_TID and beh.BEHANDLING_STATUS != 'AVSLU' and beh.ID >= :fraOgMedId
                      and beh.ID <= :tilOgMedId
                )
                order by id
            )
              where ROWNUM <= 10
              """;

        var query = entityManager.createNativeQuery(sql, Behandling.class)
            .setParameter("fraOgMedId", fraOgMedId)
            .setParameter("tilOgMedId", tilOgMedId);
        return query.getResultStream();
    }

    public static ProsessTaskData opprettNesteTask(Long nyFraOgMed, boolean dryRun, Long tilOgMed) {
        var prosessTaskData = ProsessTaskData.forProsessTask(BeregningTilbakehoppFeilretterBatchTask.class);
        prosessTaskData.setProperty(FRA_OG_MED, String.valueOf(nyFraOgMed));
        prosessTaskData.setProperty(TIL_OG_MED, String.valueOf(tilOgMed));
        prosessTaskData.setProperty(DRY_RUN, String.valueOf(dryRun));
        return prosessTaskData;
    }
}
