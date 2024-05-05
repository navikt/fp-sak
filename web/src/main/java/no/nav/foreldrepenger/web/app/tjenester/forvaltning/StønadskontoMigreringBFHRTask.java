package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.jpa.HibernateHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.stønadskonto.grensesnitt.Stønadsdager;
import no.nav.foreldrepenger.stønadskonto.regelmodell.grunnlag.LegacyGrunnlagV1;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "stønadskonto.bfhrmigrering", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class StønadskontoMigreringBFHRTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(StønadskontoMigreringBFHRTask.class);
    private static final String FRA_ID = "fraId";
    private static final String DRYRUN = "dryRun";

    private final FagsakRelasjonRepository fagsakRelasjonRepository;
    private final BehandlingRepository behandlingRepository;
    private final YtelsesFordelingRepository ytelsesFordelingRepository;
    private final UføretrygdRepository uføretrygdRepository;
    private final EntityManager entityManager;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public StønadskontoMigreringBFHRTask(FagsakRelasjonRepository fagsakRelasjonRepository,
                                         BehandlingRepository behandlingRepository,
                                         YtelsesFordelingRepository ytelsesFordelingRepository,
                                         UføretrygdRepository uføretrygdRepository,
                                         EntityManager entityManager,
                                         ProsessTaskTjeneste prosessTaskTjeneste) {
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
        this.behandlingRepository = behandlingRepository;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.uføretrygdRepository = uføretrygdRepository;
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fraId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_ID)).map(Long::valueOf).orElse(null);
        var dryrun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRYRUN)).filter("false"::equalsIgnoreCase).isEmpty(); // krever "false"

        var saker = finnNesteHundreSaker(fraId).toList();

        saker.forEach(s -> håndterUføreSak(s, dryrun));

        saker.stream()
            .max(Long::compareTo)
            .ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettNesteTask(nesteId)));
    }

    private Stream<Long> finnNesteHundreSaker(Long fraId) {
        var sql ="""
            select * from (
             select distinct b.id
             from fpsak.behandling b join fpsak.behandling_resultat br on br.behandling_id = b.id join fpsak.behandling_vedtak bv on bv.behandling_resultat_id = br.id
             where 1=1 and behandling_type = 'BT-002'
             and b.id in (
              select distinct behandling_id from fpsak.gr_ytelses_fordeling yf join fpsak.so_rettighet soor on yf.overstyrt_rettighet_id = soor.id
              where mor_uforetrygd = 'J' and aktiv = 'J'
              UNION
              select distinct behandling_id from fpsak.gr_uforetrygd where register_ufore = 'J')
             and b.id > :fraId
             order by b.id)
            where ROWNUM <= 100
            """;

        var query = entityManager.createNativeQuery(sql, Long.class)
            .setParameter("fraId", fraId == null ? 0 : fraId)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultStream();
    }

    private void håndterUføreSak(Long behandlingId, boolean dryrun) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var kontoberegningOpt = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak())
            .flatMap(FagsakRelasjon::getGjeldendeStønadskontoberegning);

        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId);
        var uføregrunnlag = uføretrygdRepository.hentGrunnlag(behandlingId);
        boolean morUfør = ytelseFordelingAggregat.map(a -> a.morMottarUføretrygd(uføregrunnlag.orElse(null))).orElse(false);

        if (kontoberegningOpt.isEmpty() || !morUfør) {
            return;
        }
        var kontoberegning = kontoberegningOpt.orElseThrow();
        var input = kontoberegning.getRegelInput();
        var stønadsdager = Stønadsdager.instance(null);
        var endret  = false;
        if (input.contains("rettighetstype") || input.contains("familiehendelsesdato")) {
            return; // V0 eller V2 - trenger ikke etterpopulere
        } else {
            var grunnlag = StandardJsonConfig.fromJson(input, LegacyGrunnlagV1.class);
            var bareFarRett = grunnlag.isFarRett() && !grunnlag.isMorRett() && !grunnlag.isFarAleneomsorg();
            var minsterettdager = stønadsdager.minsterettBareFarRett(grunnlag.getFamiliehendelsesdato(), grunnlag.getAntallBarn(), bareFarRett, morUfør, grunnlag.getDekningsgrad());
            var utenaktivitetskravdager = stønadsdager.aktivitetskravUføredager(grunnlag.getFamiliehendelsesdato(), bareFarRett, morUfør, grunnlag.getDekningsgrad());
            if (minsterettdager > 0 && utenaktivitetskravdager > 0) {
                throw new IllegalStateException("KontoMigrering: både WLB og UFO");
            } else if (utenaktivitetskravdager > 0) {
                etterpopuler(kontoberegning, StønadskontoType.UFØREDAGER, utenaktivitetskravdager, dryrun);
                endret = true;
            } else if (minsterettdager > 0) {
                etterpopuler(kontoberegning, StønadskontoType.BARE_FAR_RETT, minsterettdager, dryrun);
                endret = true;
            }
        }
        if (endret && !dryrun) {
            fagsakRelasjonRepository.persisterFlushStønadskontoberegning(kontoberegning);
        }
    }

    private void etterpopuler(Stønadskontoberegning kontoberegning, StønadskontoType stønadskontoType, int dager, boolean dryrun) {
        var finnesAllerede = kontoberegning.getStønadskontoer().stream()
            .filter(sk -> stønadskontoType.equals(sk.getStønadskontoType()))
            .findFirst();
        if (finnesAllerede.isEmpty() && dager > 0) {
            if (dryrun) {
                LOG.info("FPSAK KONTO ETTERPOPULER id {} med konto {} dager {}", kontoberegning.getId(), stønadskontoType, dager);
                return;
            }
            var nyKonto = Stønadskonto.builder()
                .medStønadskontoType(stønadskontoType)
                .medMaxDager(dager)
                .build();
            nyKonto.setStønadskontoberegning(kontoberegning);
            kontoberegning.leggTilStønadskonto(nyKonto);
        } else if (finnesAllerede.isPresent() && dager > finnesAllerede.get().getMaxDager()) {
            if (dryrun) {
                LOG.info("FPSAK KONTO ETTERPOPULER oppdater id {} med konto {} dager {}", kontoberegning.getId(), stønadskontoType, dager);
                return;
            }

            finnesAllerede.get().setMaxDager(dager);
        }
    }

    public static ProsessTaskData opprettNesteTask(Long fraVedtakId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(StønadskontoMigreringBFHRTask.class);

        prosessTaskData.setProperty(StønadskontoMigreringBFHRTask.FRA_ID, fraVedtakId == null ? null : String.valueOf(fraVedtakId));
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(150);
        return prosessTaskData;
    }
}
