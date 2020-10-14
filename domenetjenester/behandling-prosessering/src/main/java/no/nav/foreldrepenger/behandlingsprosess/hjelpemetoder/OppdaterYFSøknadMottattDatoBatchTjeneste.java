package no.nav.foreldrepenger.behandlingsprosess.hjelpemetoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchStatus;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.log.mdc.MDCOperations;

/**
 * Midlertidig batch for å utlede og lagre mottatt dato per periode i YF
 * <p>
 * Skal kjøre en gang
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

        var fagsaker = repository.hentFagsakerSomManglerSøknadMottattDato(args.getAntall());
        var kjøres = LocalDateTime.now();
        for (var fagsak : fagsaker) {
            opprettTaskGruppe(callId, kjøres, fagsak);
            kjøres = kjøres.plus(500, ChronoUnit.MILLIS);
        }

        return BATCHNAME + "-" + UUID.randomUUID();
    }

    @Override
    public BatchArguments createArguments(Map<String, String> jobArguments) {
        return new YfMottattDatoBatchArguments(jobArguments);
    }

    private void opprettTaskGruppe(String callId, LocalDateTime kjøres, FagsakIdMedBruker fagsak) {

        var behandlinger = repository.hentBehandlinger(fagsak.getFagsakId());

        var prosessTaskGruppe = new ProsessTaskGruppe();
        for (var behandling : behandlinger) {
            ProsessTaskData prosessTaskData = new ProsessTaskData(OppdaterYFSøknadMottattDatoTask.TASKTYPE);
            prosessTaskData.setBehandling(fagsak.getFagsakId(), behandling, fagsak.getAktorId());

            String nyCallId = callId + behandling;
            prosessTaskData.setCallId(nyCallId);
            prosessTaskData.setAntallFeiledeForsøk(1);
            prosessTaskData.setPrioritet(999);
            prosessTaskData.setNesteKjøringEtter(kjøres);

            prosessTaskGruppe.addNesteSekvensiell(prosessTaskData);
        }

        prosessTaskRepository.lagre(prosessTaskGruppe);
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

        public List<FagsakIdMedBruker> hentFagsakerSomManglerSøknadMottattDato(long antall) {
            var sql = "select * from " +
                "(select distinct f.id, bruker.AKTOER_ID from fagsak f " +
                "join behandling b on f.id = b.fagsak_id " +
                "left join BEHANDLING_RESULTAT br on b.id = br.behandling_id " +
                "join BRUKER bruker on f.BRUKER_ID = bruker.ID " +
                "join GR_YTELSES_FORDELING gryf on gryf.behandling_id = b.id and gryf.aktiv = 'J'" +
                "join YF_FORDELING yf on yf.id in (gryf.SO_FORDELING_ID, gryf.JUSTERT_FORDELING_ID, gryf.OVERSTYRT_FORDELING_ID) " +
                "join YF_FORDELING_PERIODE yfp on yfp.FORDELING_ID = yf.ID " +
                "where b.id = gryf.behandling_id and yfp.mottatt_dato_temp is null " +
                "and (br.behandling_resultat_type is null or br.behandling_resultat_type <> :eskluderResultat) order by f.id desc) " +
                "where ROWNUM <= :antall";

            var query = entityManager.createNativeQuery(sql);
            query.setParameter("antall", antall);
            query.setParameter("eskluderResultat", BehandlingResultatType.MERGET_OG_HENLAGT.getKode());
            var resultList = (List<Object[]>) query.getResultList();
            return resultList.stream().map(o -> new FagsakIdMedBruker(((BigDecimal) o[0]).longValue(), (String) o[1])).collect(Collectors.toList());
        }

        public List<Long> hentBehandlinger(Long fagsakId) {
            var query = entityManager.createNativeQuery(
                "SELECT beh.id from BEHANDLING beh " +
                    "join fagsak f on f.id = beh.FAGSAK_ID " +
                    "join BEHANDLING_RESULTAT br on br.behandling_id = beh.id " +
                    " WHERE f.id=:fagsakId AND beh.BEHANDLING_TYPE in (:typer) " +
                    "and br.BEHANDLING_RESULTAT_TYPE <> :eskluderResultat")
                .setParameter("fagsakId", fagsakId)
                .setParameter("eskluderResultat", BehandlingResultatType.MERGET_OG_HENLAGT.getKode());
            var behandlingstyper = BehandlingType.getYtelseBehandlingTyper().stream()
                .map(behandlingType -> behandlingType.getKode())
                .collect(Collectors.toList());
            query.setParameter("typer", behandlingstyper);
            var resultList = (List<BigDecimal>) query.getResultList();
            return resultList.stream().map(o -> o.longValue()).collect(Collectors.toList());
        }
    }

    private static class FagsakIdMedBruker {

        private final Long fagsakId;
        private final String aktorId;

        public FagsakIdMedBruker(Long fagsakId, String aktorId) {
            this.fagsakId = fagsakId;
            this.aktorId = aktorId;
        }

        public Long getFagsakId() {
            return fagsakId;
        }

        public String getAktorId() {
            return aktorId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FagsakIdMedBruker that = (FagsakIdMedBruker) o;
            return Objects.equals(fagsakId, that.fagsakId) &&
                Objects.equals(aktorId, that.aktorId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fagsakId, aktorId);
        }
    }

    private static class YfMottattDatoBatchArguments extends BatchArguments {

        private static final long DEFAULT = 1000;

        //Antall fagsaker
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
