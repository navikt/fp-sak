package no.nav.foreldrepenger.web.app.tjenester.forvaltning.fpoversikt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandling.impl.HendelseForBehandling;
import no.nav.foreldrepenger.behandling.impl.PubliserBehandlingHendelseTask;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "fpoversikt.migrering", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FpoversiktMigeringBehandlingHendelseTask implements ProsessTaskHandler {

    public static final String MIGRERING_VERSJON = "1";
    private final ProsessTaskTjeneste taskTjeneste;
    private final BehandlingRepository behandlingRepository;
    private final EntityManager entityManager;

    @Inject
    public FpoversiktMigeringBehandlingHendelseTask(ProsessTaskTjeneste taskTjeneste,
                                                    BehandlingRepository behandlingRepository,
                                                    EntityManager entityManager) {
        this.taskTjeneste = taskTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var kanditater = finnNesteKandidater();
        kanditater.forEach(fagsakId -> {
            opprettBehandlingHendelseTask(fagsakId);
            markerSakMigrert(fagsakId);
        });
        if (!kanditater.isEmpty()) {
//            kjørVidere();
        }
    }

    private void markerSakMigrert(Long fagsakId) {
        entityManager.createNativeQuery("insert into FPOVERSIKT_FAGSAK_MIGRERING values (SEQ_FPOVERSIKT_FAGSAK_MIGRERING.nextval, :migreringVersjon, :id)")
            .setParameter("migreringVersjon", MIGRERING_VERSJON)
            .setParameter("id", fagsakId)
            .executeUpdate();
    }

    private List<Long> finnNesteKandidater() {
        return entityManager.createNativeQuery("""
            select f.id from fagsak f
            where not exists (select 1 from fpoversikt_fagsak_migrering m where m.VERSJON=:migreringVersjon and m.fagsak_id = f.id)
            FETCH FIRST 20 ROWS ONLY
            """)
            .setParameter("migreringVersjon", MIGRERING_VERSJON)
            .getResultList()
            .stream()
            .map(bd -> ((BigDecimal)bd).longValue())
            .toList();
    }

    private void opprettBehandlingHendelseTask(Long fagsakId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(PubliserBehandlingHendelseTask.class);
        var behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(fagsakId);
        if (behandlingOpt.isEmpty()) {
            return;
        }
        prosessTaskData.setBehandling(fagsakId, behandlingOpt.orElseThrow().getId());
        //Tar i bruk BRUKEROPPGAVE siden den ikke lyttes på av noen andre konsumenter
        prosessTaskData.setProperty(PubliserBehandlingHendelseTask.HENDELSE_TYPE, HendelseForBehandling.BRUKEROPPGAVE.name());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(90);
        taskTjeneste.lagre(prosessTaskData);
    }

    private void kjørVidere() {
        var prosessTaskData = migreringTaskData();
        taskTjeneste.lagre(prosessTaskData);
    }

    static ProsessTaskData migreringTaskData() {
        var prosessTaskData = ProsessTaskData.forProsessTask(FpoversiktMigeringBehandlingHendelseTask.class);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(100);
        prosessTaskData.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(2));
        return prosessTaskData;
    }

}
