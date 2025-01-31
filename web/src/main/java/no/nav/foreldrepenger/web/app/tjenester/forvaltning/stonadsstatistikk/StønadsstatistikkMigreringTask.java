package no.nav.foreldrepenger.web.app.tjenester.forvaltning.stonadsstatistikk;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkKafkaProducer;
import no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkTjeneste;
import no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaSender;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@Dependent
@ProsessTask(value = "stønadsstatisikk.migrering", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class StønadsstatistikkMigreringTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(StønadsstatistikkMigreringTask.class);
    static final String FOM_DATO_KEY = "fom";
    private static final String FRA_ID = "fraId";

    private final BehandlingRepository behandlingRepository;
    private final StønadsstatistikkTjeneste stønadsstatistikkTjeneste;
    private final SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private final Validator validator;
    private final StønadsstatistikkKafkaProducer kafkaProducer;
    private final EntityManager entityManager;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public StønadsstatistikkMigreringTask(BehandlingRepository behandlingRepository,
                                          StønadsstatistikkTjeneste stønadsstatistikkTjeneste,
                                          SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                          StønadsstatistikkKafkaProducer kafkaProducer,
                                          EntityManager entityManager,
                                          ProsessTaskTjeneste prosessTaskTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.stønadsstatistikkTjeneste = stønadsstatistikkTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.kafkaProducer = kafkaProducer;
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;

        @SuppressWarnings("resource") var factory = Validation.buildDefaultValidatorFactory();
        // hibernate validator implementations er thread-safe, trenger ikke close
        validator = factory.getValidator();
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var vedtakFomDato = LocalDate.parse(prosessTaskData.getPropertyValue(FOM_DATO_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
        var fraIdProperty = prosessTaskData.getPropertyValue(FRA_ID);
        var fraId = fraIdProperty == null ? null : Long.valueOf(fraIdProperty);


        var vedtakOpprettetFomDato = finnVedtakOpprettetFomDato(vedtakFomDato, fraId).toList();
        var dtos = vedtakOpprettetFomDato.stream()
            .sorted(Comparator.comparing(BehandlingVedtak::getOpprettetTidspunkt))
            .flatMap(bv -> lagDto(bv).stream())
            .toList();

        dtos.forEach(v -> {
            var header = new KafkaSender.KafkaHeader("stønadstype", v.getYtelseType().name().getBytes());
            kafkaProducer.sendJson(header, v.getSaksnummer().id(), DefaultJsonMapper.toJson(v));
        });

        vedtakOpprettetFomDato.stream()
            .map(BehandlingVedtak::getId)
            .max(Long::compareTo)
            .ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettTaskForDato(vedtakFomDato, nesteId)));

    }

    private Stream<BehandlingVedtak> finnVedtakOpprettetFomDato(LocalDate fomDato, Long fraId) {
        var sql ="""
            select * from (
            select bv.* from BEHANDLING_VEDTAK bv
            join BEHANDLING_RESULTAT br on br.id = bv.BEHANDLING_RESULTAT_ID
            join BEHANDLING b on b.id = br.BEHANDLING_ID
            where b.BEHANDLING_TYPE in ('BT-002','BT-004')
            and trunc(bv.OPPRETTET_TID) >=:fomDato
            and bv.ID >:fraId
            order by bv.id)
            where ROWNUM <= 20
            """;

        var query = entityManager.createNativeQuery(sql, BehandlingVedtak.class)
            .setParameter("fomDato", fomDato)
            .setParameter(FRA_ID, fraId == null ? 0 : fraId)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultStream();
    }

    private Optional<StønadsstatistikkVedtak> lagDto(BehandlingVedtak vedtak) {
        var behandlingId = vedtak.getBehandlingsresultat().getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        return Optional.of(lagDto(behandling, vedtak));
    }

    private StønadsstatistikkVedtak lagDto(Behandling behandling, BehandlingVedtak vedtak) {
        LOG.info("Produserer stønadsstatistikk for sak {} behandling {} vedtak {} fattet {}", behandling.getSaksnummer().getVerdi(), behandling.getId(),
            vedtak.getId(), vedtak.getOpprettetTidspunkt());
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var generertVedtak = stønadsstatistikkTjeneste.genererVedtak(BehandlingReferanse.fra(behandling), stp);
        var violations = validator.validate(generertVedtak);
        if (!violations.isEmpty()) {
            // Har feilet validering
            var allErrors = violations.stream().map(it -> it.getPropertyPath().toString() + " :: " + it.getMessage()).toList();
            throw new IllegalArgumentException("stønadsstatistikk valideringsfeil \n " + allErrors);
        }
        return generertVedtak;
    }

    public static ProsessTaskData opprettTaskForDato(LocalDate fomDato, Long fraVedtakId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(StønadsstatistikkMigreringTask.class);

        prosessTaskData.setProperty(StønadsstatistikkMigreringTask.FOM_DATO_KEY, fomDato.toString());
        prosessTaskData.setProperty(StønadsstatistikkMigreringTask.FRA_ID, Optional.ofNullable(fraVedtakId).map(String::valueOf).orElse(null));

        return prosessTaskData;
    }
}
