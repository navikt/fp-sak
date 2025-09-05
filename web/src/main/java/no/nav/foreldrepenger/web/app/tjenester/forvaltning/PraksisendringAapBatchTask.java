package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.prosess.AapPraksisendringTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@ProsessTask(value = "aap.praksisendring.batch", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class PraksisendringAapBatchTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PraksisendringAapBatchTask.class);
    private static final String FRA_OG_MED = "fraOgMed";
    private static final String TIL_OG_MED = "tilOgMed";
    private static final String DRY_RUN = "dryRun";

    private EntityManager entityManager;
    private AapPraksisendringTjeneste aapPraksisendringTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;


    public PraksisendringAapBatchTask() {
        // For CDI
    }

    @Inject
    public PraksisendringAapBatchTask(EntityManager entityManager,
                                      AapPraksisendringTjeneste aapPraksisendringTjeneste,
                                      ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.aapPraksisendringTjeneste = aapPraksisendringTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fraOgMedId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_OG_MED)).map(Long::valueOf).orElseThrow();
        var tilOgMedId = Optional.ofNullable(prosessTaskData.getPropertyValue(TIL_OG_MED)).map(Long::valueOf).orElseThrow();
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).filter("false"::equalsIgnoreCase).isEmpty();

        var saker = finnNesteTiSaker(fraOgMedId, tilOgMedId);

        saker.forEach(sak -> opprettTaskForSak(sak, dryRun));

        saker.stream()
            .map(Fagsak::getId)
            .max(Long::compareTo)
            .ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettNesteTask(nesteId + 1, dryRun, tilOgMedId)));
    }

    private List<Fagsak> finnNesteTiSaker(Long fraOgMedId, Long tilOgMedId) {
        var query = entityManager.createNativeQuery("""
            select * from (select * from fagsak where id in (select distinct(fag.id) from Fagsak fag
             inner join Behandling beh on beh.fagsak_id = fag.id
             inner join Aksjonspunkt ap on ap.behandling_id = beh.id
             where ap.aksjonspunkt_def = '5052' and ap.aksjonspunkt_status = 'UTFO'
             and fag.id >= :fraOgMedId and fag.id <= :tilOgMedId) order by id)
             where ROWNUM <= 10""", Fagsak.class)
            .setParameter("fraOgMedId", fraOgMedId)
            .setParameter("tilOgMedId", tilOgMedId);
        return query.getResultList();
    }

    private void opprettTaskForSak(Fagsak fagsak, boolean dryRun) {
        if (dryRun) {
            var erPåvirket = aapPraksisendringTjeneste.erPåvirketAvPraksisendring(fagsak.getId());
            LOG.info("PÅVIRKET_AV_AAP_PRAKSISENDRING: {} fagsakId {}", erPåvirket, fagsak.getId());
        } else {
            // TODO lag tasker for å opprette revurderinger, tas i neste omgang når vi ser hvor mange det gjelder.
        }
    }

    public static ProsessTaskData opprettNesteTask(Long nyFraOgMed, boolean dryRun, Long tilOgMed) {
        var prosessTaskData = ProsessTaskData.forProsessTask(PraksisendringAapBatchTask.class);
        prosessTaskData.setProperty(FRA_OG_MED, String.valueOf(nyFraOgMed));
        prosessTaskData.setProperty(TIL_OG_MED, String.valueOf(tilOgMed));
        prosessTaskData.setProperty(DRY_RUN, String.valueOf(dryRun));
        return prosessTaskData;
    }
}
