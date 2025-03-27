package no.nav.foreldrepenger.domene.vedtak.migrering;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ProsessTask(value = "fpinntektsmelding.opprettManglendeforespørsler", prioritet = 3, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettManglendeForespørslerTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OpprettManglendeForespørslerTask.class);
    private static final String FRA_FAGSAK_ID = "fraFagsakId";
    private static final String TOM_FAGSAK_ID = "tomFagsakId";
    private static final String DRY_RUN = "dryRun";

    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private MigrerManglendeForespørslerTjeneste migrerManglendeForespørslerTjeneste;

    OpprettManglendeForespørslerTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettManglendeForespørslerTask(EntityManager entityManager,
                                            ProsessTaskTjeneste prosessTaskTjeneste,
                                            MigrerManglendeForespørslerTjeneste migrerManglendeForespørslerTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.migrerManglendeForespørslerTjeneste = migrerManglendeForespørslerTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fraFagsakId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_FAGSAK_ID)).map(Long::valueOf).orElseThrow();
        var tomFagsakId = Optional.ofNullable(prosessTaskData.getPropertyValue(TOM_FAGSAK_ID)).map(Long::valueOf).orElse(2188628L);
        var dryRun = Boolean.parseBoolean(Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).orElse("true"));

        var saker = finnNesteHundreSaker(fraFagsakId, tomFagsakId);

        saker.forEach(sak -> {
            migrerManglendeForespørslerTjeneste.vurderOmForespørselSkalOpprettes(sak, dryRun);
        });

        saker.stream()
            .map(Fagsak::getId)
            .max(Long::compareTo)
            .ifPresent(sisteId -> prosessTaskTjeneste.lagre(opprettManglendeForespørselTaskForNesteDato(sisteId+1, tomFagsakId, dryRun)));
    }

    private List<Fagsak> finnNesteHundreSaker(Long nesteFagsakId, Long tomFagsakId) {
        var sql = """
            select * from (
            select fag.* from FAGSAK fag
            where fag.id >= :fraFagsakId and fag.id <= :tomFagsakId
            and fag.fagsak_status = :lopendeStatus
            and fag.ytelse_type in (:ytelseTyper)
            order by fag.id)
            where ROWNUM <= 100
            """;

        var query = entityManager.createNativeQuery(sql, Fagsak.class)
            .setParameter("fraFagsakId", nesteFagsakId)
            .setParameter("tomFagsakId", tomFagsakId)
            .setParameter("lopendeStatus", FagsakStatus.LØPENDE.getKode())
            .setParameter("ytelseTyper", List.of(FagsakYtelseType.SVANGERSKAPSPENGER.getKode(), FagsakYtelseType.FORELDREPENGER.getKode()));

        return query.getResultList();
    }

    public static ProsessTaskData opprettManglendeForespørselTaskForNesteDato(Long nesteId, Long tomFagsakId,  boolean dryRun) {
        LOG.info("MIGRER-FP: Oppretter OpprettManglendeForespørslerTask for fagsakId {}", nesteId);
        var prosessTaskData = ProsessTaskData.forProsessTask(OpprettManglendeForespørslerTask.class);
        prosessTaskData.setProperty(FRA_FAGSAK_ID, String.valueOf(nesteId));
        prosessTaskData.setProperty(TOM_FAGSAK_ID, String.valueOf(tomFagsakId));
        prosessTaskData.setProperty(DRY_RUN, String.valueOf(dryRun));
        return prosessTaskData;
    }
}
