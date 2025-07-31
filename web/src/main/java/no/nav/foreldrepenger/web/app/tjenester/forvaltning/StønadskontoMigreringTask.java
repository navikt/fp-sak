package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.stønadskonto.grensesnitt.Stønadsdager;
import no.nav.foreldrepenger.stønadskonto.regelmodell.grunnlag.LegacyGrunnlagV0;
import no.nav.foreldrepenger.stønadskonto.regelmodell.grunnlag.LegacyGrunnlagV1;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

/*
 * Migreringspattern til senere bruk. Skal ikke kjøres på nytt. Bevares for pattern
 */
@Dependent
@ProsessTask(value = "stønadskonto.migrering", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class StønadskontoMigreringTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(StønadskontoMigreringTask.class);
    private static final String FRA_ID = "fraId";
    private static final String MAX_ID = "maxId";
    private static final String DRY_RUN = "dryRun";


    private final FagsakRelasjonRepository fagsakRelasjonRepository;
    private final EntityManager entityManager;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public StønadskontoMigreringTask(FagsakRelasjonRepository fagsakRelasjonRepository,
                                     EntityManager entityManager,
                                     ProsessTaskTjeneste prosessTaskTjeneste) {
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fraId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_ID)).map(Long::valueOf).orElse(null);
        var maxId = Optional.ofNullable(prosessTaskData.getPropertyValue(MAX_ID)).map(Long::valueOf).orElse(null);
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).filter("false"::equalsIgnoreCase).isEmpty();

        var beregninger = finnNesteHundreStønadskonti(fraId).toList();

        beregninger.stream().filter(b -> maxId == null || b.getId() < maxId).forEach(ur -> håndterBeregning(ur, dryRun));

        beregninger.stream()
            .map(Stønadskontoberegning::getId)
            .max(Long::compareTo)
            .filter(v -> maxId == null || v < maxId)
            .ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettNesteTask(nesteId, dryRun, maxId)));
    }

    private Stream<Stønadskontoberegning> finnNesteHundreStønadskonti(Long fraId) {
        var sql ="""
            select * from (
            select sk.* from STOENADSKONTOBEREGNING sk
            where sk.ID >:fraId
            order by sk.id)
            where ROWNUM <= 100
            """;

        var query = entityManager.createNativeQuery(sql, Stønadskontoberegning.class)
            .setParameter(FRA_ID, fraId == null ? 0 : fraId);
        return query.getResultStream();
    }

    private void håndterBeregning(Stønadskontoberegning kontoberegning, boolean dryRun) {
        var input = kontoberegning.getRegelInput();
        var endret  = false;
        if (dryRun) {
            // DO som logging
            return;
        }
        if (input.contains("rettighetstype")) {
            return; // V2 - trenger ikke etterpopulere
        } else if (input.contains("familiehendelsesdato")) {
            var grunnlag = StandardJsonConfig.fromJson(input, LegacyGrunnlagV0.class);
            if (grunnlag.getAntallBarn() > 1) {
                //etterpopuler(kontoberegning, StønadskontoType.TILLEGG_FLERBARN, dager);
                endret = true;
            }
        } else {
            var grunnlag = StandardJsonConfig.fromJson(input, LegacyGrunnlagV1.class);
            if (grunnlag.getAntallBarn() > 1) {
                //etterpopuler(kontoberegning, StønadskontoType.TILLEGG_FLERBARN, dager);
                endret = true;
            }
            if (grunnlag.erFødsel() && grunnlag.getFødselsdato().isPresent() && grunnlag.getTermindato().isPresent()) {
                //etterpopuler(kontoberegning, StønadskontoType.TILLEGG_PREMATUR, prematur);
                endret = true;
            }
            if (grunnlag.erFødsel() && grunnlag.isMinsterett() && grunnlag.isFarRett()) {
                //etterpopuler(kontoberegning, StønadskontoType.FAR_RUNDT_FØDSEL, rundtFødsel);
                endret = true;
            }
            if (grunnlag.isMinsterett() && grunnlag.isFarRett() && !grunnlag.isMorRett() && !grunnlag.isFarAleneomsorg()) {
                //etterpopuler(kontoberegning, StønadskontoType.BARE_FAR_RETT, rundtFødsel);
                endret = true;
            }
        }
        if (endret) {
            fagsakRelasjonRepository.persisterFlushStønadskontoberegning(kontoberegning);
        }
    }

    @SuppressWarnings("unused")
    private void etterpopuler(Stønadskontoberegning kontoberegning, StønadskontoType stønadskontoType, int dager) {
        var finnesAllerede = kontoberegning.getStønadskontoer().stream().map(Stønadskonto::getStønadskontoType).anyMatch(stønadskontoType::equals);
        if (!finnesAllerede && dager > 0) {
            LOG.info("FPSAK KONTO ETTERPOPULER id {} med konto {} dager {}", kontoberegning.getId(), stønadskontoType, dager);
            var nyKonto = Stønadskonto.builder()
                .medStønadskontoType(stønadskontoType)
                .medMaxDager(dager)
                .build();
            nyKonto.setStønadskontoberegning(kontoberegning);
            kontoberegning.leggTilStønadskonto(nyKonto);
        }
    }

    public static ProsessTaskData opprettNesteTask(Long fraVedtakId, boolean dryRun, Long maxId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(StønadskontoMigreringTask.class);

        prosessTaskData.setProperty(StønadskontoMigreringTask.FRA_ID, fraVedtakId == null ? null : String.valueOf(fraVedtakId));
        prosessTaskData.setProperty(StønadskontoMigreringTask.MAX_ID, maxId == null ? null : String.valueOf(maxId));
        prosessTaskData.setProperty(StønadskontoMigreringTask.DRY_RUN, String.valueOf(dryRun));
        return prosessTaskData;
    }
}
