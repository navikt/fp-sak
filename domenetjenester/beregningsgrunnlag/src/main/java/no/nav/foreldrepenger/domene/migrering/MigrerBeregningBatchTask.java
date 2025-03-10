package no.nav.foreldrepenger.domene.migrering;

import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

/*
 * Migreringspattern til senere bruk. Skal ikke kjøres på nytt. Bevares for pattern
 */
@Dependent
@ProsessTask(value = "beregningsgrunnlag.migrer.batch", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class MigrerBeregningBatchTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MigrerBeregningBatchTask.class);
    private static final String FRA_OG_MED = "fraOgMed";
    private static final String TIL_OG_MED = "tilOgMed";
    private static final String DRY_RUN = "dryRun";


    private final BeregningMigreringTjeneste beregningMigreringTjeneste;
    private final EntityManager entityManager;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public MigrerBeregningBatchTask(BeregningMigreringTjeneste beregningMigreringTjeneste,
                                    EntityManager entityManager,
                                    ProsessTaskTjeneste prosessTaskTjeneste) {
        this.beregningMigreringTjeneste = beregningMigreringTjeneste;
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fraOgMedId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_OG_MED)).map(Long::valueOf).orElseThrow();
        var tilOgMedId = Optional.ofNullable(prosessTaskData.getPropertyValue(TIL_OG_MED)).map(Long::valueOf).orElseThrow();
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).filter("false"::equalsIgnoreCase).isEmpty();

        var saker = finnNesteHundreSaker(fraOgMedId, tilOgMedId).toList();

        saker.forEach(sak -> håndterBeregning(sak, dryRun));

        saker.stream()
            .map(Fagsak::getId)
            .max(Long::compareTo)
            .ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettNesteTask(nesteId+1, dryRun, tilOgMedId)));
    }

    private Stream<Fagsak> finnNesteHundreSaker(Long fraOgMedId, Long tilOgMedId) {
        var sql ="""
            select * from (select fag.* from FAGSAK fag
            where fag.ID >= :fraOgMedId and fag.ID <= :tilOgMedId
            order by fag.id)
            where ROWNUM <= 10
            """;

        var query = entityManager.createNativeQuery(sql, Fagsak.class)
            .setParameter("fraOgMedId", fraOgMedId)
            .setParameter("tilOgMedId", tilOgMedId);
        return query.getResultStream();
    }

    private void håndterBeregning(Fagsak fagsak, boolean dryRun) {
        if (dryRun) {
            LOG.info("Dry run: Migrerer ikke saksnummer {}", fagsak.getSaksnummer());
            return;
        }
        beregningMigreringTjeneste.migrerSak(fagsak.getSaksnummer());
    }

    public static ProsessTaskData opprettNesteTask(Long nyFraOgMed, boolean dryRun, Long tilOgMed) {
        var prosessTaskData = ProsessTaskData.forProsessTask(MigrerBeregningBatchTask.class);
        prosessTaskData.setProperty(FRA_OG_MED, String.valueOf(nyFraOgMed));
        prosessTaskData.setProperty(TIL_OG_MED, String.valueOf(tilOgMed));
        prosessTaskData.setProperty(DRY_RUN, String.valueOf(dryRun));
        return prosessTaskData;
    }
}
