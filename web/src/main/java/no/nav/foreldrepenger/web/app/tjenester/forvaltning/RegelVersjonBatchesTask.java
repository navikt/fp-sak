package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;
import java.time.Month;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/*
 * Engangstask. Slett etter bruk
 */
@Dependent
@ProsessTask(value = "migrering.regelversjon.batch", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class RegelVersjonBatchesTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RegelVersjonBatchesTask.class);

    private final EntityManager entityManager;

    @Inject
    public RegelVersjonBatchesTask(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        oppdaterInngangsvilkår();
        oppdaterInngangsvilkårBeregning();
        slettProsesstaskUtenGruppe();
    }

    private void oppdaterInngangsvilkår() {
        var sql ="""
            update vilkar
            set regel_versjon = 'fp-inngangsvilkar:1.0.0'
            where coalesce(endret_tid,opprettet_tid) > :tidspunkt and regel_versjon is null and regel_evaluering like '%"inngangsvilkår"%'
            """;

        var antall = entityManager.createNativeQuery(sql)
            .setParameter("tidspunkt", LocalDate.of(2024, Month.JUNE, 15).atStartOfDay())
            .executeUpdate();
        LOG.info("FPSAK REGEL IV {}", antall);
    }

    private void oppdaterInngangsvilkårBeregning() {
        var sql ="""
            update vilkar
            set regel_versjon = 'ft-beregning:'||cast(substr(substr(regel_evaluering, instr(regel_evaluering, '"beregningsgrunnlag"') + 24), 0, instr(substr(regel_evaluering, instr(regel_evaluering, '"beregningsgrunnlag"') + 24), '"') - 1) as varchar2(50))
            where vilkar_type = 'FP_VK_41' and coalesce(endret_tid, opprettet_tid) > :tidspunkt and regel_versjon is null and regel_evaluering like '%"beregningsgrunnlag"%'
            """;

        var antall = entityManager.createNativeQuery(sql)
            .setParameter("tidspunkt", LocalDate.of(2023, Month.FEBRUARY, 10).atStartOfDay())
            .executeUpdate();
        LOG.info("FPSAK REGEL IV {}", antall);
    }

    private void slettProsesstaskUtenGruppe() {
        entityManager.createNativeQuery("DELETE from prosess_task where task_gruppe is null and status = 'FERDIG''")
            .executeUpdate();
    }


}
