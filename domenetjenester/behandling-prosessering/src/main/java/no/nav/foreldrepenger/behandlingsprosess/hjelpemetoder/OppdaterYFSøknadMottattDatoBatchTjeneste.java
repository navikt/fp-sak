package no.nav.foreldrepenger.behandlingsprosess.hjelpemetoder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchStatus;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.log.mdc.MDCOperations;

/**
 * Midlertidig batch for å utlede og lagre mottatt dato per periode i YF
 *
 * Skal kjøre en gang
 *
 */
@ApplicationScoped
public class OppdaterYFSøknadMottattDatoBatchTjeneste implements BatchTjeneste {

    private static final String BATCHNAME = "BVL2394";

    private OppdaterMottattDatoRepositoryRepository repository;
    private ProsessTaskRepository prosessTaskRepository;

    @Inject
    public OppdaterYFSøknadMottattDatoBatchTjeneste(OppdaterMottattDatoRepositoryRepository repository,
                                                    ProsessTaskRepository prosessTaskRepository) {
        this.repository = repository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    OppdaterYFSøknadMottattDatoBatchTjeneste() {
        //CDI
    }

    @Override
    public String launch(BatchArguments arguments) {
        var args = (YfMottattDatoBatchArguments) arguments;

        String callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";

        var behandlinger = repository.hentBehandlingerSomManglerSøknadMottattDato(args.getAntall());
        var kjøres = LocalDateTime.now();
        for (var behandling : behandlinger) {
            opprettRekjøringsTask(behandling, callId, kjøres);
            kjøres = kjøres.plus(250, ChronoUnit.MILLIS);
        }

        return BATCHNAME + "-" + UUID.randomUUID();
    }

    @Override
    public BatchArguments createArguments(Map<String, String> jobArguments) {
        return new YfMottattDatoBatchArguments(jobArguments);
    }

    private void opprettRekjøringsTask(Behandling behandling, String callId, LocalDateTime kjøres) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(OppdaterYFSøknadMottattDatoTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        // unik per task da det er ulike tasks for hver behandling
        String nyCallId = callId + behandling.getId();
        prosessTaskData.setCallId(nyCallId);
        prosessTaskData.setAntallFeiledeForsøk(1);
        prosessTaskData.setPrioritet(999);
        prosessTaskData.setNesteKjøringEtter(kjøres);

        prosessTaskRepository.lagre(prosessTaskData);
    }

    @Override
    public BatchStatus status(String batchInstanceNumber) {
        return BatchStatus.OK;
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }

    @ApplicationScoped
    public static class OppdaterMottattDatoRepositoryRepository {

        private EntityManager entityManager;

        @Inject
        public OppdaterMottattDatoRepositoryRepository(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        OppdaterMottattDatoRepositoryRepository() {
            //CDI
        }

        public List<Behandling> hentBehandlingerSomManglerSøknadMottattDato(long antall) {
            var sql = "select * from " +
                "(select distinct b.* from behandling b " +
                "join GR_YTELSES_FORDELING gryf on gryf.behandling_id = b.id and gryf.aktiv = 'J'" +
                "join YF_FORDELING yf on yf.id in (gryf.SO_FORDELING_ID, gryf.JUSTERT_FORDELING_ID, gryf.OVERSTYRT_FORDELING_ID) " +
                "join YF_FORDELING_PERIODE yfp on yfp.FORDELING_ID = yf.ID " +
                "where b.id = gryf.behandling_id and yfp.mottatt_dato_temp is null " +
                "order by fagsak_id) " +
                "where ROWNUM <= :antall";

            Query query = entityManager.createNativeQuery(sql, Behandling.class);
            query.setParameter("antall", antall);
            return ((List<Behandling>) query.getResultList());
        }
    }

    private static class YfMottattDatoBatchArguments extends BatchArguments {

        private static final long DEFAULT = 1000;

        //Antall behandlinger
         private Long antall;

        public YfMottattDatoBatchArguments(Map<String, String> arguments) {
            super(arguments);
        }

        @Override
        public boolean settParameterVerdien(String key, String value) {
            if (key.equals("antall")) {
                antall = Long.parseLong(value);
            }
            return true;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        public long getAntall() {
            return antall == null ? DEFAULT : antall;
        }
    }
}
