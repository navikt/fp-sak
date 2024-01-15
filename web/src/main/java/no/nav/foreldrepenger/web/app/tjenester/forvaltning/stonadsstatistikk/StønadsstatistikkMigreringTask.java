package no.nav.foreldrepenger.web.app.tjenester.forvaltning.stonadsstatistikk;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "stønadsstatisikk.migrering", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class StønadsstatistikkMigreringTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(StønadsstatistikkMigreringTask.class);
    static final String DATO_KEY = "dato";
    static final String SISTE_DATO = "sistedato";
    static final String SEKUNDER_PAUSE = "sekunderPause";

    private final BehandlingRepository behandlingRepository;
    private final EntityManager entityManager;
    private final StønadsstatistikkTjeneste stønadsstatistikkTjeneste;
    private final SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private final BehandlingVedtakRepository behandlingVedtakRepository;
    private final ProsessTaskTjeneste prosessTaskTjeneste;
    private Validator validator;

    @Inject
    public StønadsstatistikkMigreringTask(BehandlingRepository behandlingRepository,
                                          EntityManager entityManager,
                                          StønadsstatistikkTjeneste stønadsstatistikkTjeneste,
                                          SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                          BehandlingVedtakRepository behandlingVedtakRepository,
                                          ProsessTaskTjeneste prosessTaskTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.entityManager = entityManager;
        this.stønadsstatistikkTjeneste = stønadsstatistikkTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;

        @SuppressWarnings("resource") var factory = Validation.buildDefaultValidatorFactory();
        // hibernate validator implementations er thread-safe, trenger ikke close
        validator = factory.getValidator();
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dato = LocalDate.parse(prosessTaskData.getPropertyValue(DATO_KEY), DateTimeFormatter.ISO_LOCAL_DATE);

        var saker = finnSakerOpprettetPåDato(dato);
        LOG.info("Produserer stønadsstatistikk for saker opprettet {} {}", dato, saker.size());
        saker.forEach(this::produser);
        opprettNesteTask(prosessTaskData, dato);
    }

    private void opprettNesteTask(ProsessTaskData prosessTaskData, LocalDate dato) {
        var sisteDato = LocalDate.parse(prosessTaskData.getPropertyValue(SISTE_DATO), DateTimeFormatter.ISO_LOCAL_DATE);
        if (dato.isBefore(sisteDato)) {
            var delay = Integer.parseInt(prosessTaskData.getPropertyValue(SEKUNDER_PAUSE));
            var nyTask = opprettTaskForDato(dato.plusDays(1), sisteDato, delay);
            prosessTaskTjeneste.lagre(nyTask);
        }
    }

    static ProsessTaskData opprettTaskForDato(LocalDate dato, LocalDate sisteDato, int delay) {
        LOG.info("Produserer stønadsstatistikk oppretter task for dato {}", dato);
        var prosessTaskData = ProsessTaskData.forProsessTask(StønadsstatistikkMigreringTask.class);
        prosessTaskData.setProperty(StønadsstatistikkMigreringTask.DATO_KEY, dato.toString());
        prosessTaskData.setProperty(StønadsstatistikkMigreringTask.SEKUNDER_PAUSE, String.valueOf(delay));
        prosessTaskData.setProperty(StønadsstatistikkMigreringTask.SISTE_DATO, sisteDato.toString());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(150);
        prosessTaskData.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(delay));
        return prosessTaskData;
    }

    private List<Long> finnSakerOpprettetPåDato(LocalDate dato) {
        var sql = """
            select f.id from fagsak f
            where trunc(opprettet_tid) =:dato and til_infotrygd = 'N'
            """;
        var query = entityManager.createNativeQuery(sql).setParameter("dato", dato);
        return query.getResultList().stream().map(num -> Long.parseLong(num.toString())).toList();
    }

    private void produser(Long fagsakId) {
        var vedtak = behandlingVedtakRepository.hentGjeldendeVedtak(fagsakId);
        vedtak.ifPresentOrElse(this::produser, () -> LOG.info("Produserer stønadsstatistikk ingen gjeldende vedtak for fagsak {}", fagsakId));
    }

    private void produser(BehandlingVedtak vedtak) {
        var behandlingId = vedtak.getBehandlingsresultat().getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        try {
            produser(vedtak, behandling);
        } catch (Exception e) {
            LOG.info("Produserer stønadsstatistikk feilet {} behandling {} vedtak {}", behandling.getFagsak().getSaksnummer(), behandling.getId(),
                vedtak.getId(), e);
        }
    }

    private void produser(BehandlingVedtak vedtak, Behandling behandling) {
        LOG.info("Produserer stønadsstatistikk for sak {} behandling {} vedtak {}", behandling.getFagsak().getSaksnummer(), behandling.getId(),
            vedtak.getId());
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var generertVedtak = stønadsstatistikkTjeneste.genererVedtak(BehandlingReferanse.fra(behandling, stp));
        var violations = validator.validate(generertVedtak);
        if (!violations.isEmpty()) {
            // Har feilet validering
            var allErrors = violations.stream().map(it -> it.getPropertyPath().toString() + " :: " + it.getMessage()).toList();
            throw new IllegalArgumentException("stønadsstatistikk valideringsfeil \n " + allErrors);
        }
    }
}
