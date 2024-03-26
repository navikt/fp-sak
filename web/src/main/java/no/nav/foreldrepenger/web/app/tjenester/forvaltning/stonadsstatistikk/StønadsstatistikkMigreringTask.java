package no.nav.foreldrepenger.web.app.tjenester.forvaltning.stonadsstatistikk;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
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
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@Dependent
@ProsessTask(value = "stønadsstatisikk.migrering", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class StønadsstatistikkMigreringTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(StønadsstatistikkMigreringTask.class);
    static final String FOM_DATO_KEY = "fom";
    static final String TOM_DATO_KEY = "tom";
    private static final String FRA_FAGSAK_ID = "fraFagsakId";
    private static final LocalDateTime MAKSTIDSPUNKT_VEDTAK = LocalDate.of(2024, Month.APRIL, 1).atStartOfDay();

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
        var sakOpprettetTid = LocalDate.parse(prosessTaskData.getPropertyValue(FOM_DATO_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
        var fagsakIdProperty = prosessTaskData.getPropertyValue(FRA_FAGSAK_ID);
        var fraFagsakId = fagsakIdProperty == null ? null : Long.valueOf(fagsakIdProperty);

        var vedtakPåDato = finnAlleVedtakForSakerOpprettetDato(sakOpprettetTid, fraFagsakId);
        LOG.info("Publiserer migreringshendelse for {} saker opprettet {}", vedtakPåDato.size(), sakOpprettetTid);
        var dtos = vedtakPåDato.stream()
            .flatMap(this::finnVedtakForSak)
            .sorted(Comparator.comparing(BehandlingVedtak::getOpprettetTidspunkt))
            .flatMap(bv -> lagDto(bv).stream())
            .toList();

        dtos.forEach(v -> kafkaProducer.sendJson(v.getSaksnummer().id(), DefaultJsonMapper.toJson(v)));

        var tomDato = LocalDate.parse(prosessTaskData.getPropertyValue(TOM_DATO_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
        vedtakPåDato.stream()
            .max(Long::compareTo)
            .ifPresentOrElse(nesteId -> prosessTaskTjeneste.lagre(opprettTaskForDato(sakOpprettetTid, tomDato, nesteId)),
                () -> {
                    if (sakOpprettetTid.isBefore(tomDato)) {
                        prosessTaskTjeneste.lagre(opprettTaskForDato(sakOpprettetTid.plusDays(1), tomDato, null));
                    }
                });

    }

    private List<Long> finnAlleVedtakForSakerOpprettetDato(LocalDate sakOpprettetTid, Long fraFagsakId) {
        var sql ="""
            select * from (select f.id from FAGSAK f
            where trunc(f.OPPRETTET_TID) =:sakOpprettetTid
            and f.id >:fraFagsakId
            order by f.id)
            where ROWNUM <= 20
            """;

        var query = entityManager.createNativeQuery(sql, Long.class)
            .setParameter("sakOpprettetTid", sakOpprettetTid)
            .setParameter("fraFagsakId", fraFagsakId == null ? 0 : fraFagsakId)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultList();
    }

    private Stream<BehandlingVedtak> finnVedtakForSak(Long fagsakId) {
        var sql ="""
            select bv.* from BEHANDLING_VEDTAK bv
            join BEHANDLING_RESULTAT br on br.id = bv.BEHANDLING_RESULTAT_ID
            join BEHANDLING b on b.id = br.BEHANDLING_ID
            where b.BEHANDLING_TYPE in ('BT-002','BT-004')
            and b.FAGSAK_ID =:fagsakId
            """;

        var query = entityManager.createNativeQuery(sql, BehandlingVedtak.class)
            .setParameter("fagsakId", fagsakId)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultStream();
    }

    private Optional<StønadsstatistikkVedtak> lagDto(BehandlingVedtak vedtak) {
        if (vedtak.getOpprettetTidspunkt().isAfter(MAKSTIDSPUNKT_VEDTAK)) {
            LOG.info("Ignorerer vedtak {}. Fattet {}", vedtak.getId(), vedtak.getOpprettetTidspunkt());
            return Optional.empty();
        }
        var behandlingId = vedtak.getBehandlingsresultat().getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        return Optional.of(lagDto(behandling, vedtak));
    }

    private StønadsstatistikkVedtak lagDto(Behandling behandling, BehandlingVedtak vedtak) {
        LOG.info("Produserer stønadsstatistikk for sak {} behandling {} vedtak {} fattet {}", behandling.getFagsak().getSaksnummer().getVerdi(), behandling.getId(),
            vedtak.getId(), vedtak.getOpprettetTidspunkt());
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var generertVedtak = stønadsstatistikkTjeneste.genererVedtak(BehandlingReferanse.fra(behandling, stp));
        var violations = validator.validate(generertVedtak);
        if (!violations.isEmpty()) {
            // Har feilet validering
            var allErrors = violations.stream().map(it -> it.getPropertyPath().toString() + " :: " + it.getMessage()).toList();
            throw new IllegalArgumentException("stønadsstatistikk valideringsfeil \n " + allErrors);
        }
        return generertVedtak;
    }

    public static ProsessTaskData opprettTaskForDato(LocalDate fomDato, LocalDate tomDato, Long fraVedtakId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(StønadsstatistikkMigreringTask.class);

        prosessTaskData.setProperty(StønadsstatistikkMigreringTask.FOM_DATO_KEY, fomDato.toString());
        prosessTaskData.setProperty(StønadsstatistikkMigreringTask.TOM_DATO_KEY, tomDato.toString());
        prosessTaskData.setProperty(StønadsstatistikkMigreringTask.FRA_FAGSAK_ID, fraVedtakId == null ? null : String.valueOf(fraVedtakId));
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(150);
        return prosessTaskData;
    }
}
