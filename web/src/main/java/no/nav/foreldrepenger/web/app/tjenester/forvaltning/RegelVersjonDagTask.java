package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

/*
 * Engangstask. Slett etter bruk
 */
@Dependent
@ProsessTask(value = "migrering.regelversjon.dag", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class RegelVersjonDagTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RegelVersjonDagTask.class);

    private static final String DATO = "dato";

    private final EntityManager entityManager;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public RegelVersjonDagTask(EntityManager entityManager, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dato = Optional.ofNullable(prosessTaskData.getPropertyValue(DATO)).map(s -> LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE))
            .orElseGet(() -> LocalDate.of(2023, Month.FEBRUARY, 6));

        oppdaterStønadskontoKonto(dato);
        oppdaterUttakResultatPeriode(dato);
        oppdaterInngangsvilkårBeregning(dato);
        oppdaterBeregningsGrunnlag(dato);
        oppdaterBeregningsGrunnlagPeriode(dato);

        var nyTaskData = ProsessTaskData.forProsessTask(RegelVersjonDagTask.class);
        nyTaskData.setProperty(RegelVersjonDagTask.DATO, dato.plusDays(1).toString());
        nyTaskData.setCallIdFraEksisterende();
        prosessTaskTjeneste.lagre(nyTaskData);
    }

    private void oppdaterInngangsvilkårBeregning(LocalDate dato) {
        var sql ="""
            update vilkar
            set regel_versjon = 'ft-beregning:'||cast(substr(substr(regel_evaluering, instr(regel_evaluering, '"beregningsgrunnlag"') + 24), 0, instr(substr(regel_evaluering, instr(regel_evaluering, '"beregningsgrunnlag"') + 24), '"') - 1) as varchar2(50))
            where vilkar_type = 'FP_VK_41' and opprettet_tid >= :tidspunktFra and opprettet_tid < :tidspunktTil and regel_versjon is null and regel_evaluering like '%"beregningsgrunnlag"%'
            """;

        var antall = entityManager.createNativeQuery(sql)
            .setParameter("tidspunktFra", dato.atStartOfDay())
            .setParameter("tidspunktTil", dato.plusDays(1).atStartOfDay())
            .executeUpdate();
        LOG.info("FPSAK REGEL IVBER {} {}", dato, antall);
    }

    private void oppdaterStønadskontoKonto(LocalDate dato) {
        var sql ="""
            update STOENADSKONTOBEREGNING
            set regel_versjon = 'fp-stonadskonto:'||cast(substr(regel_evaluering, instr(regel_evaluering, '"fp-stonadskonto"') + 21, 5) as varchar2(50))
            where opprettet_tid >= :tidspunktFra and opprettet_tid < :tidspunktTil and regel_versjon is null and regel_evaluering like '%"fp-stonadskonto"%'
            """;

        var antall = entityManager.createNativeQuery(sql)
            .setParameter("tidspunktFra", dato.atStartOfDay())
            .setParameter("tidspunktTil", dato.plusDays(1).atStartOfDay())
            .executeUpdate();
        LOG.info("FPSAK REGEL KontoKonto  {} {}", dato, antall);
    }

    private void oppdaterUttakResultatPeriode(LocalDate dato) {
        var sql ="""
            update UTTAK_RESULTAT_DOK_REGEL
            set regel_versjon = 'fp-uttak:'||cast(substr(substr(regel_evaluering, instr(regel_evaluering, '"fp-uttak"') + 14), 0, instr(substr(regel_evaluering, instr(regel_evaluering, '"fp-uttak"') + 14), '"') - 1) as varchar2(30))
            where opprettet_tid >= :tidspunktFra and opprettet_tid < :tidspunktTil and regel_versjon is null and regel_evaluering like '%"fp-uttak"%'
            """;

        var antall = entityManager.createNativeQuery(sql)
            .setParameter("tidspunktFra", dato.atStartOfDay())
            .setParameter("tidspunktTil", dato.plusDays(1).atStartOfDay())
            .executeUpdate();
        LOG.info("FPSAK REGEL UR dato {} {}", dato, antall);
    }

    private void oppdaterBeregningsGrunnlag(LocalDate dato) {
        var sql ="""
            update BG_REGEL_SPORING
            set regel_versjon ='ft-beregning:'||cast(substr(substr(regel_evaluering, instr(regel_evaluering, '"beregningsgrunnlag"') + 24), 0, instr(substr(regel_evaluering, instr(regel_evaluering, '"beregningsgrunnlag"') + 24), '"') - 1) as varchar2(30))
            where opprettet_tid >= :tidspunktFra and opprettet_tid < :tidspunktTil and regel_versjon is null and regel_evaluering like '%"beregningsgrunnlag"%'
            """;

        var antall = entityManager.createNativeQuery(sql)
            .setParameter("tidspunktFra", dato.atStartOfDay())
            .setParameter("tidspunktTil", dato.plusDays(1).atStartOfDay())
            .executeUpdate();
        LOG.info("FPSAK REGEL BG dato {} {}", dato, antall);
    }

    private void oppdaterBeregningsGrunnlagPeriode(LocalDate dato) {
        var sql ="""
            update BG_PERIODE_REGEL_SPORING
            set regel_versjon ='ft-beregning:'||cast(substr(substr(regel_evaluering, instr(regel_evaluering, '"beregningsgrunnlag"') + 24), 0, instr(substr(regel_evaluering, instr(regel_evaluering, '"beregningsgrunnlag"') + 24), '"') - 1) as varchar2(30))
            where opprettet_tid >= :tidspunktFra and opprettet_tid < :tidspunktTil and regel_versjon is null and regel_evaluering like '%"beregningsgrunnlag"%'
            )
            """;

        var antall = entityManager.createNativeQuery(sql)
            .setParameter("tidspunktFra", dato.atStartOfDay())
            .setParameter("tidspunktTil", dato.plusDays(1).atStartOfDay())
            .executeUpdate();
        LOG.info("FPSAK REGEL BGP dato {} {}", dato, antall);
    }

}
