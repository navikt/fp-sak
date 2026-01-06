package no.nav.foreldrepenger.web.app.tjenester.los;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;

import org.hibernate.jpa.HibernateHints;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ProsessTask(value = "los.sendbehandling.alle", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class MigrerÅpneBehandlingerTilLosBatchTask implements ProsessTaskHandler {

    private static final String FRA_OG_MED = "fraBehandlingId";
    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;


    public MigrerÅpneBehandlingerTilLosBatchTask() {
        // For CDI
    }

    @Inject
    public MigrerÅpneBehandlingerTilLosBatchTask(EntityManager entityManager,
                                                 ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fraOgMedId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_OG_MED)).map(Long::valueOf).orElse(0L);

        var behandlinger = finnNesteHundreSaker(fraOgMedId);

        var gruppe = new ProsessTaskGruppe();
        gruppe.addNesteParallell(behandlinger.stream().map(this::opprettTaskForBehandling).toList());
        prosessTaskTjeneste.lagre(gruppe);

        behandlinger.stream()
            .max(Long::compareTo)
            .ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettNesteTask(nesteId + 1)));
    }

    private List<Long> finnNesteHundreSaker(Long fraOgMedId) {
        var query = entityManager.createNativeQuery("""
            select behandlingId from (
                select distinct behandling_id as behandlingId from aksjonspunkt
                where behandling_id >= :fraId AND AKSJONSPUNKT_STATUS = 'OPPR' AND AKSJONSPUNKT_DEF <> '7013'
                order by behandlingId
            )
             where ROWNUM <= 100""")
            .setHint(HibernateHints.HINT_READ_ONLY, "true")
            .setParameter("fraId", fraOgMedId);
        @SuppressWarnings("unchecked")
        List<Number> resultater = query.getResultList();
        return resultater.stream().map(Number::longValue).toList();
    }

    private ProsessTaskData opprettTaskForBehandling(Long behandlingId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(MigrerBehandlingTilLosTask.class);
        prosessTaskData.setProperty(MigrerBehandlingTilLosTask.SEND_BEHANDLING_ID, String.valueOf(behandlingId));
        return prosessTaskData;
    }

    public static ProsessTaskData opprettNesteTask(Long nyFraOgMed) {
        var prosessTaskData = ProsessTaskData.forProsessTask(MigrerÅpneBehandlingerTilLosBatchTask.class);
        prosessTaskData.setProperty(FRA_OG_MED, String.valueOf(nyFraOgMed));
        prosessTaskData.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(3));
        return prosessTaskData;
    }
}
