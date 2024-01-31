package no.nav.foreldrepenger.web.app.tjenester.forvaltning.stonadsstatistikk;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.hibernate.jpa.HibernateHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkKafkaProducer;
import no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@Dependent
@ProsessTask(value = "stønadsstatisikk.migrering", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class StønadsstatistikkMigreringTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(StønadsstatistikkMigreringTask.class);
    static final String DATO_KEY = "dato";

    private final BehandlingRepository behandlingRepository;
    private final StønadsstatistikkTjeneste stønadsstatistikkTjeneste;
    private final SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private final Validator validator;
    private final StønadsstatistikkKafkaProducer kafkaProducer;
    private final EntityManager entityManager;

    @Inject
    public StønadsstatistikkMigreringTask(BehandlingRepository behandlingRepository,
                                          StønadsstatistikkTjeneste stønadsstatistikkTjeneste,
                                          SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                          StønadsstatistikkKafkaProducer kafkaProducer,
                                          EntityManager entityManager) {
        this.behandlingRepository = behandlingRepository;
        this.stønadsstatistikkTjeneste = stønadsstatistikkTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.kafkaProducer = kafkaProducer;
        this.entityManager = entityManager;

        @SuppressWarnings("resource") var factory = Validation.buildDefaultValidatorFactory();
        // hibernate validator implementations er thread-safe, trenger ikke close
        validator = factory.getValidator();
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fom = LocalDate.parse(prosessTaskData.getPropertyValue(DATO_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
        LOG.info("Publiser migreringshendelse for vedtak opprettet {}", fom);

        var vedtak = finnAlleVedtakForDato(fom);

        LOG.info("Publiser migreringshendelse for {} vedtak", vedtak.size());
        vedtak.forEach(this::produser);
    }

    private List<BehandlingVedtak> finnAlleVedtakForDato(LocalDate vedtaksdato) {
        var query = entityManager.createQuery("FROM BehandlingVedtak bv where bv.behandlingsresultat.behandling.behandlingType in (:behnadlingstyper) and trunc(bv.opprettetTidspunkt) =:vedtaksdato", BehandlingVedtak.class)
            .setParameter("vedtaksdato", vedtaksdato)
            .setParameter("behnadlingstyper", BehandlingType.getYtelseBehandlingTyper())
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultList();
    }

    private void produser(BehandlingVedtak vedtak) {
        var behandlingId = vedtak.getBehandlingsresultat().getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        produser(behandling, vedtak.getId());
    }

    private void produser(Behandling behandling, Long vedtakId) {
        LOG.info("Produserer stønadsstatistikk for sak {} behandling {} vedtak {}", behandling.getFagsak().getSaksnummer(), behandling.getId(),
            vedtakId);
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var generertVedtak = stønadsstatistikkTjeneste.genererVedtak(BehandlingReferanse.fra(behandling, stp));
        var violations = validator.validate(generertVedtak);
        if (!violations.isEmpty()) {
            // Har feilet validering
            var allErrors = violations.stream().map(it -> it.getPropertyPath().toString() + " :: " + it.getMessage()).toList();
            throw new IllegalArgumentException("stønadsstatistikk valideringsfeil \n " + allErrors);
        }
        kafkaProducer.sendJson(behandling.getFagsak().getSaksnummer().getVerdi(), DefaultJsonMapper.toJson(generertVedtak));
    }
}
