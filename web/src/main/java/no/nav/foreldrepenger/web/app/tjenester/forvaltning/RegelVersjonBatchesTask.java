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
        oppdaterTilkjent();
        oppdaterFeriepenger();
        oppdaterSvangerskapspenger();
        oppdaterStønadskontoUttak();
        oppdaterStønadskontoKonto();
    }

    private void oppdaterInngangsvilkår() {
        var sql ="""
            update vilkar
            set regel_versjon = 'fp-inngangsvilkar:1.0.0'
            where opprettet_tid > :tidspunkt and regel_versjon is null and regel_evaluering like '%"inngangsvilkår"%'
            """;

        var antall = entityManager.createNativeQuery(sql)
            .setParameter("tidspunkt", LocalDate.of(2024, Month.JUNE, 15).atStartOfDay())
            .executeUpdate();
        LOG.info("FPSAK REGEL IV {}", antall);
    }

    private void oppdaterTilkjent() {
        var sql ="""
            update BR_BEREGNINGSRESULTAT
            set regel_versjon = 'fp-ytelse-beregn:1.0.2'
            where opprettet_tid > :tidspunkt and regel_versjon is null and regel_sporing like '%"beregningsresultat" : "UNKNOWN"%'
            """;

        var antall = entityManager.createNativeQuery(sql)
            .setParameter("tidspunkt", LocalDate.of(2024, Month.JUNE, 16).atStartOfDay())
            .executeUpdate();
        LOG.info("FPSAK REGEL Tilkjent {}", antall);
    }

    private void oppdaterFeriepenger() {
        var sql ="""
            update BR_FERIEPENGER
            set regel_versjon = 'fp-ytelse-beregn:1.0.2'
            where opprettet_tid > :tidspunkt and regel_versjon is null and feriepenger_regel_sporing like '%"beregningsresultat" : "UNKNOWN"%'
            """;

        var antall = entityManager.createNativeQuery(sql)
            .setParameter("tidspunkt", LocalDate.of(2024, Month.JUNE, 16).atStartOfDay())
            .executeUpdate();
        LOG.info("FPSAK REGEL Ferie {}", antall);
    }

    private void oppdaterSvangerskapspenger() {
        var sql ="""
            update SVP_UTTAK_PERIODE
            set regel_versjon = 'svp-uttak:'||cast(substr(substr(regel_evaluering, instr(regel_evaluering, '"svp-uttak"') + 15), 0, instr(substr(regel_evaluering, instr(regel_evaluering, '"svp-uttak"') + 15), '"') - 1) as varchar2(50))
            where opprettet_tid > :tidspunkt and regel_versjon is null and regel_evaluering like '%"svp-uttak"%'
            """;

        var antall = entityManager.createNativeQuery(sql)
            .setParameter("tidspunkt", LocalDate.of(2023, Month.FEBRUARY, 1).atStartOfDay())
            .executeUpdate();
        LOG.info("FPSAK REGEL SVP {}", antall);
    }

    private void oppdaterStønadskontoUttak() {
        var sql ="""
            update STOENADSKONTOBEREGNING
            set regel_versjon = 'fp-uttak:'||cast(substr(regel_evaluering, instr(regel_evaluering, '"fp-uttak"') + 14, 6) as varchar2(50))
            where opprettet_tid > :tidspunktFra and opprettet_tid < :tidspunktTil and regel_versjon is null and regel_evaluering like '%"fp-uttak"%'
            """;

        var antall = entityManager.createNativeQuery(sql)
            .setParameter("tidspunktFra", LocalDate.of(2023, Month.FEBRUARY, 1).atStartOfDay())
            .setParameter("tidspunktTil", LocalDate.of(2023, Month.MARCH, 2).atStartOfDay())
            .executeUpdate();
        LOG.info("FPSAK REGEL KontoUttak {}", antall);
    }

}
