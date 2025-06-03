package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "beregningsgrunnlag.tilbakerulling.batch", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class TilbakerullBeregningsgrunnlagBatchTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TilbakerullBeregningsgrunnlagBatchTask.class);
    private static final String FRA_OG_MED = "fraOgMed";
    private static final String TIL_OG_MED = "tilOgMed";
    private static final String AKSJONSPUNKT_KODE = "apKode";
    private static final String DRY_RUN = "dryRun";

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingProsesseringTjeneste prosesseringTjeneste;
    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;

    @Inject
    public TilbakerullBeregningsgrunnlagBatchTask(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                                  BehandlingProsesseringTjeneste prosesseringTjeneste,
                                                  EntityManager entityManager,
                                                  ProsessTaskTjeneste prosessTaskTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
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
        var apKode = Optional.ofNullable(prosessTaskData.getPropertyValue(AKSJONSPUNKT_KODE)).map(Long::valueOf).orElseThrow();

        var behandlinger = finnNesteHundreSaker(fraOgMedId, tilOgMedId, apKode).toList();

        if (dryRun) {
            LOG.info("Fant {} behandlinger som kan rulles tilbake, men gjøres ikke i dryRun", behandlinger.size());
        } else {
            LOG.info("Fant {} behandlinger som kan rulles tilbake", behandlinger.size());
            behandlinger.forEach(this::rullTilbakeBehandling);
        }

        behandlinger.stream()
            .map(Behandling::getId)
            .max(Long::compareTo)
            .ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettNesteTask(nesteId+1, dryRun, tilOgMedId, apKode)));
    }

    private void rullTilbakeBehandling(Behandling behandling) {
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling, lås);
        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }
        var tilSteg = FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType()) ? BehandlingStegType.VURDER_SAMLET : BehandlingStegType.DEKNINGSGRAD;
        lagHistorikkinnslag(behandling, tilSteg.getNavn());
        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, tilSteg);
        prosesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, LocalDateTime.now());
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

    private Stream<Behandling> finnNesteHundreSaker(Long fraOgMedId, Long tilOgMedId, Long aksjonspunktKode) {
        var sql = """
              select * from (
                select *
                from behandling
                where id in (
                    select distinct beh.id
                    from behandling beh
                             inner join aksjonspunkt ap on ap.behandling_id = beh.id
                             inner join gr_beregningsgrunnlag gr on gr.behandling_id = beh.id
                    where beh.ID >= :fraOgMedId
                      and beh.ID <= :tilOgMedId
                      and gr.aktiv = 'J'
                      and ap.aksjonspunkt_def = :apKode
                      and beh.behandling_status = 'UTRED'
                      and ap.aksjonspunkt_status = 'OPPR'
                )
                order by id
            )
              where ROWNUM <= 10
              """;

        var query = entityManager.createNativeQuery(sql, Behandling.class)
            .setParameter("fraOgMedId", fraOgMedId)
            .setParameter("tilOgMedId", tilOgMedId)
            .setParameter("apKode", aksjonspunktKode);
        return query.getResultStream();
    }

    public static ProsessTaskData opprettNesteTask(Long nyFraOgMed, boolean dryRun, Long tilOgMed, Long aksjonspunktKode) {
        var prosessTaskData = ProsessTaskData.forProsessTask(TilbakerullBeregningsgrunnlagBatchTask.class);
        prosessTaskData.setProperty(FRA_OG_MED, String.valueOf(nyFraOgMed));
        prosessTaskData.setProperty(TIL_OG_MED, String.valueOf(tilOgMed));
        prosessTaskData.setProperty(AKSJONSPUNKT_KODE, String.valueOf(aksjonspunktKode));
        prosessTaskData.setProperty(DRY_RUN, String.valueOf(dryRun));
        return prosessTaskData;
    }
}
