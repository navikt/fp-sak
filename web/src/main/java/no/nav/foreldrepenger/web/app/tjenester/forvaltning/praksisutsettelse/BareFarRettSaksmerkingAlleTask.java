package no.nav.foreldrepenger.web.app.tjenester.forvaltning.praksisutsettelse;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.jpa.HibernateHints;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "behandling.saksmerkebarefarrett.alle", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class BareFarRettSaksmerkingAlleTask implements ProsessTaskHandler {

    private static final String FRA_FAGSAK_ID = "fraFagsakId";
    private final EntityManager entityManager;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    public enum Utvalg { MOR, FAR_BEGGE_RETT }

    @Inject
    public BareFarRettSaksmerkingAlleTask(EntityManager entityManager,
                                          ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fagsakIdProperty = prosessTaskData.getPropertyValue(FRA_FAGSAK_ID);
        var fraFagsakId = fagsakIdProperty == null ? null : Long.valueOf(fagsakIdProperty);
        var saker = finnNesteHundreSakerForBareFarRettEllerAlene(fraFagsakId);
        saker.stream().map(BareFarRettSaksmerkingAlleTask::opprettTaskForEnkeltSak).forEach(prosessTaskTjeneste::lagre);

        saker.stream().max(Comparator.naturalOrder())
            .map(BareFarRettSaksmerkingAlleTask::opprettTaskForNesteUtvalg)
            .ifPresent(prosessTaskTjeneste::lagre);

    }

    public static ProsessTaskData opprettTaskForEnkeltSak(Long fagsakId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(BareFarRettSaksmerkingSingleTask.class);
        prosessTaskData.setProperty(FeilPraksisSaksmerkingSingleTask.FAGSAK_ID, String.valueOf(fagsakId));
        return prosessTaskData;
    }


    public static ProsessTaskData opprettTaskForNesteUtvalg(Long fraFagsakId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(BareFarRettSaksmerkingAlleTask.class);
        prosessTaskData.setProperty(BareFarRettSaksmerkingAlleTask.FRA_FAGSAK_ID, fraFagsakId == null ? null : String.valueOf(fraFagsakId));
        prosessTaskData.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(30));
        return prosessTaskData;
    }


    private static final String QUERY_MERKING_FAR_BEGGE_ALENE = """
        select * from (
          select fid from (
            select distinct f.id as fid from fpsak.STOENADSKONTO sk
            join fpsak.fagsak_relasjon fr on sk.stoenadskontoberegning_id = coalesce(overstyrt_konto_beregning_id, konto_beregning_id)
            join fpsak.fagsak f on f.id in (fr.fagsak_en_id, fr.fagsak_to_id)
            where stoenadskontotype = 'FORELDREPENGER' and fr.aktiv = 'J' and bruker_rolle <> 'MORA' and f.id >:fraFagsakId
            union
            select distinct f.id as fid from fpsak.STOENADSKONTO sk
            join fpsak.uttak_resultat ur on sk.stoenadskontoberegning_id = ur.konto_beregning_id
            join fpsak.behandling_resultat br on ur.behandling_resultat_id = br.id
            join fpsak.behandling b on br.behandling_id = b.id
            join fpsak.fagsak f on b.fagsak_id = f.id
            where stoenadskontotype = 'FORELDREPENGER' and ur.aktiv = 'J' and bruker_rolle <> 'MORA' and behandling_resultat_type not like '%ENLAG%'
            and f.id >:fraFagsakId
          ) order by fid
        ) where ROWNUM <= 100
        """;

    public List<Long> finnNesteHundreSakerForBareFarRettEllerAlene(Long fraFagsakId) {
        var query = entityManager.createNativeQuery(QUERY_MERKING_FAR_BEGGE_ALENE)
            .setParameter("fraFagsakId", fraFagsakId == null ? 0 : fraFagsakId)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        @SuppressWarnings("unchecked")
        var resultat = query.getResultList();
        return resultat;
    }
}
