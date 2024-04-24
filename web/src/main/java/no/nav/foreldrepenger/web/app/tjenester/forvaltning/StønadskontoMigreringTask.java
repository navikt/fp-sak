package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.jpa.HibernateHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkKafkaProducer;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.stønadskonto.grensesnitt.Stønadsdager;
import no.nav.foreldrepenger.stønadskonto.regelmodell.grunnlag.LegacyGrunnlagV0;
import no.nav.foreldrepenger.stønadskonto.regelmodell.grunnlag.LegacyGrunnlagV1;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "stønadskonto.migrering", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class StønadskontoMigreringTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(StønadskontoMigreringTask.class);
    private static final String FRA_ID = "fraId";

    private final FagsakRelasjonRepository fagsakRelasjonRepository;
    private final StønadsstatistikkKafkaProducer kafkaProducer;
    private final EntityManager entityManager;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public StønadskontoMigreringTask(FagsakRelasjonRepository fagsakRelasjonRepository,
                                     StønadsstatistikkKafkaProducer kafkaProducer,
                                     EntityManager entityManager,
                                     ProsessTaskTjeneste prosessTaskTjeneste) {
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
        this.kafkaProducer = kafkaProducer;
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fraIdProperty = prosessTaskData.getPropertyValue(FRA_ID);
        var fraId = fraIdProperty == null ? null : Long.valueOf(fraIdProperty);

        var beregninger = finnNesteHundreStønadskonti(fraId).toList();

        beregninger.forEach(this::håndterBeregning);

        beregninger.stream()
            .map(Stønadskontoberegning::getId)
            .max(Long::compareTo)
            .ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettNesteTask(nesteId)));
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
            .setParameter("fraId", fraId == null ? 0 : fraId)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultStream();
    }

    private void håndterBeregning(Stønadskontoberegning kontoberegning) {
        var input = kontoberegning.getRegelInput();
        var stønadsdager = Stønadsdager.instance(null);
        var endret  = false;
        if (input.contains("rettighetstype")) {
            return; // V2 - trenger ikke etterpopulere
        } else if (input.contains("familiehendelsesdato")) {
            var grunnlag = StandardJsonConfig.fromJson(input, LegacyGrunnlagV0.class);
            if (grunnlag.getAntallBarn() > 1) {
                var dager = stønadsdager.ekstradagerFlerbarn(grunnlag.getFamiliehendelsesdato(), grunnlag.getAntallBarn(), grunnlag.getDekningsgrad());
                etterpopuler(kontoberegning, StønadskontoType.TILLEGG_FLERBARN, dager);
                endret = true;
            }
        } else {
            var grunnlag = StandardJsonConfig.fromJson(input, LegacyGrunnlagV1.class);
            if (grunnlag.getAntallBarn() > 1) {
                var dager = stønadsdager.ekstradagerFlerbarn(grunnlag.getFamiliehendelsesdato(), grunnlag.getAntallBarn(), grunnlag.getDekningsgrad());
                etterpopuler(kontoberegning, StønadskontoType.TILLEGG_FLERBARN, dager);
                endret = true;
            }
            if (grunnlag.erFødsel() && grunnlag.getFødselsdato().isPresent() && grunnlag.getTermindato().isPresent()) {
                var prematur = stønadsdager.ekstradagerPrematur(grunnlag.getFødselsdato().orElseThrow(), grunnlag.getTermindato().orElseThrow());
                etterpopuler(kontoberegning, StønadskontoType.TILLEGG_PREMATUR, prematur);
                endret = true;
            }
            if (grunnlag.erFødsel() && grunnlag.isMinsterett() && grunnlag.isFarRett()) {
                var rundtFødsel = stønadsdager.andredagerFarRundtFødsel(grunnlag.getFamiliehendelsesdato(), true);
                etterpopuler(kontoberegning, StønadskontoType.FAR_RUNDT_FØDSEL, rundtFødsel);
                endret = true;
            }
            if (grunnlag.isMinsterett() && grunnlag.isFarRett() && !grunnlag.isMorRett() && !grunnlag.isFarAleneomsorg()) {
                var rundtFødsel = stønadsdager.minsterettBareFarRett(grunnlag.getFamiliehendelsesdato(), grunnlag.getAntallBarn(), true, false, grunnlag.getDekningsgrad());
                etterpopuler(kontoberegning, StønadskontoType.BARE_FAR_RETT, rundtFødsel);
                endret = true;
            }
        }
        if (endret) {
            fagsakRelasjonRepository.persisterFlushStønadskontoberegning(kontoberegning);
        }
    }

    private void etterpopuler(Stønadskontoberegning kontoberegning, StønadskontoType stønadskontoType, int dager) {
        var finnesAllerede = kontoberegning.getStønadskontoer().stream().map(Stønadskonto::getStønadskontoType).anyMatch(stønadskontoType::equals);
        if (!finnesAllerede && dager > 0) {
            var nyKonto = Stønadskonto.builder()
                .medStønadskontoType(stønadskontoType)
                .medMaxDager(dager)
                .build();
            nyKonto.setStønadskontoberegning(kontoberegning);
            kontoberegning.getStønadskontoer().add(nyKonto);
        }
    }

    public static ProsessTaskData opprettNesteTask(Long fraVedtakId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(StønadskontoMigreringTask.class);

        prosessTaskData.setProperty(StønadskontoMigreringTask.FRA_ID, fraVedtakId == null ? null : String.valueOf(fraVedtakId));
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(150);
        return prosessTaskData;
    }
}
