package no.nav.foreldrepenger.domene.uttak;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@ProsessTask(OppdaterSøknadMottattDatoTask.TASKTYPE)
public class OppdaterSøknadMottattDatoTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(OppdaterSøknadMottattDatoTask.class);
    public static final String TASKTYPE = "oppdater.yf.soknad.mottattdato";

    private static Duration DELAY_MELLOM_KJØRINGER = Duration.ofSeconds(1);

    private OppdaterMottattDatoRepositoryRepository repository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private BehandlingLåsRepository behandlingLåsRepository;
    private SøknadRepository søknadRepository;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;


    @Inject
    public OppdaterSøknadMottattDatoTask(OppdaterMottattDatoRepositoryRepository repository,
                                         YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                         ProsessTaskRepository prosessTaskRepository,
                                         BehandlingLåsRepository behandlingLåsRepository,
                                         SøknadRepository søknadRepository,
                                         UttaksperiodegrenseRepository uttaksperiodegrenseRepository) {
        this.repository = repository;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingLåsRepository = behandlingLåsRepository;
        this.søknadRepository = søknadRepository;
        this.uttaksperiodegrenseRepository = uttaksperiodegrenseRepository;
    }

    OppdaterSøknadMottattDatoTask() {
        // CDI krav
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        LOG.info("Kjører task for å oppdatere mottatt dato");
        var behandlinger = repository.hentBehandlingerSomManglerSøknadMottattDato();
        for (var behandling : behandlinger) {
            LOG.info("Oppdaterer mottatt dato for behandling {}", behandling.getId());
            try {
                oppdaterMottattDatoForBehandling(behandling);
            } catch (Throwable e) {
                LOG.info("Klarte ikke å oppdatere mottatt dato for behandling {} {}", behandling.getId(), behandling.getFagsak().getSaksnummer());
                throw e;
            }
        }

        if (!behandlinger.isEmpty()) {
            startTask(prosessTaskRepository);
        } else {
            LOG.info("siste {} er ferdig", TASKTYPE);
        }
    }

    private void oppdaterMottattDatoForBehandling(Behandling behandling) {
        var behandlingLås = behandlingLåsRepository.taLås(behandling.getId());
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandling.getId());

        var oppgittFordeling = ytelseFordelingAggregat.getOppgittFordeling();
        var justertFordeling = ytelseFordelingAggregat.getJustertFordeling();
        var overstyrtFordeling = ytelseFordelingAggregat.getOverstyrtFordeling();

        oppgittFordeling.getOppgittePerioder().forEach(p -> oppdaterMottattDato(p, behandling));
        justertFordeling.ifPresent(f -> f.getOppgittePerioder().forEach(p -> oppdaterMottattDato(p, behandling)));
        overstyrtFordeling.ifPresent(f -> f.getOppgittePerioder().forEach(p -> oppdaterMottattDato(p, behandling)));
        behandlingLåsRepository.oppdaterLåsVersjon(behandlingLås);
        repository.flush();
    }

    private void oppdaterMottattDato(OppgittPeriodeEntitet periode, Behandling behandling) {
        var mottattDato = utledMottattDato(periode, behandling);
        if (mottattDato == null) {
            throw new IllegalStateException("Kunne ikke utlede mottatt dato for behandling " + behandling.getId());
        }
        periode.setMottattDatoTemp(mottattDato);
        repository.persist(periode);
    }

    private LocalDate utledMottattDato(OppgittPeriodeEntitet periode, Behandling behandling) {
        var tidligstBehandlingMedPeriode = finnTidligstBehandling(periode, behandling);
        var uttaksperiodegrense = uttaksperiodegrenseRepository.hentHvisEksisterer(tidligstBehandlingMedPeriode.getId());
        if (uttaksperiodegrense.isPresent()) {
            return uttaksperiodegrense.get().getMottattDato();
        }
        return søknadRepository.hentSøknad(tidligstBehandlingMedPeriode).getMottattDato();
    }

    private Behandling finnTidligstBehandling(OppgittPeriodeEntitet periode, Behandling behandling) {
        var originalBehandling = behandling.getOriginalBehandling();
        var førsteBehandlingMedPeriode = behandling;
        while (originalBehandling.isPresent()) {
            if (finnesIBehandling(periode, originalBehandling.get())) {
                førsteBehandlingMedPeriode = originalBehandling.get();
            }
            originalBehandling = originalBehandling.get().getOriginalBehandling();
        }
        return førsteBehandlingMedPeriode;
    }

    private boolean finnesIBehandling(OppgittPeriodeEntitet periode, Behandling behandling) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandling.getId());
        if (ytelseFordelingAggregat.isEmpty()) {
            return false;
        }
        var oppgittFordeling = ytelseFordelingAggregat.get().getOppgittFordeling();

        return oppgittFordeling.getOppgittePerioder().stream().anyMatch(op -> lik(periode, op));
    }

    private boolean lik(OppgittPeriodeEntitet periode1, OppgittPeriodeEntitet periode2) {
        return periode1.getFom().equals(periode2.getFom())
            && periode1.getTom().equals(periode2.getTom())
            && Objects.equals(periode1.getÅrsak(), periode2.getÅrsak())
            && Objects.equals(periode1.getArbeidsprosent(), periode2.getArbeidsprosent())
            && periode1.getErArbeidstaker() == periode2.getErArbeidstaker()
            && Objects.equals(periode1.getArbeidsgiver(), periode2.getArbeidsgiver())
            && Objects.equals(periode1.getPeriodeType(), periode2.getPeriodeType())
            && (Objects.equals(periode1.getSamtidigUttaksprosent(), periode2.getSamtidigUttaksprosent())
            || periode1.getSamtidigUttaksprosent().compareTo(periode2.getSamtidigUttaksprosent()) == 0)
            ;
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

        public List<Behandling> hentBehandlingerSomManglerSøknadMottattDato() {
            var sql = "select * from " +
                "(select b.* from behandling b " +
                "join GR_YTELSES_FORDELING gryf on gryf.behandling_id = b.id and gryf.aktiv = 'J'" +
                "join YF_FORDELING yf on yf.id in (gryf.SO_FORDELING_ID, gryf.JUSTERT_FORDELING_ID, gryf.OVERSTYRT_FORDELING_ID) " +
                "join YF_FORDELING_PERIODE yfp on yfp.FORDELING_ID = yf.ID " +
                "where b.id = gryf.behandling_id and yfp.mottatt_dato_temp is null) " +
                "where ROWNUM <= :antall";

            Query query = entityManager.createNativeQuery(sql, Behandling.class);
            query.setParameter("antall", 10);
            return ((List<Behandling>) query.getResultList());
        }

        public void persist(OppgittPeriodeEntitet periode) {
            entityManager.persist(periode);
        }

        public void flush() {
            entityManager.flush();
        }
    }

    public static void startTask(ProsessTaskRepository prosessTaskRepository) {
        ProsessTaskData data = new ProsessTaskData(TASKTYPE);
        data.setNesteKjøringEtter(LocalDateTime.now().plus(DELAY_MELLOM_KJØRINGER));
        prosessTaskRepository.lagre(data);
    }
}
