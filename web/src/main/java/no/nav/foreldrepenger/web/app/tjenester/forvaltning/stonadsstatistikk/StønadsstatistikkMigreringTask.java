package no.nav.foreldrepenger.web.app.tjenester.forvaltning.stonadsstatistikk;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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
    static final String DATO_KEY = "dato";
    private static final String FRA_VEDTAK_ID = "fraVedtakId";

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
        var vedtaksdato = LocalDate.parse(prosessTaskData.getPropertyValue(DATO_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
        var vedtakIdProperty = prosessTaskData.getPropertyValue(FRA_VEDTAK_ID);
        var fraVedtakId = vedtakIdProperty == null ? null : Long.valueOf(vedtakIdProperty);

        var vedtakPåDato = finnAlleVedtakForDato(vedtaksdato, fraVedtakId);
        var dtos = vedtakPåDato.stream()
            .map(this::lagDto)
            .collect(Collectors.toSet());
        LOG.info("Publiserer migreringshendelse for {} vedtak opprettet {}", dtos.size(), vedtaksdato);

        dtos.forEach(v -> kafkaProducer.sendJson(v.getSaksnummer().id(), DefaultJsonMapper.toJson(v)));

        vedtakPåDato.stream()
            .map(BehandlingVedtak::getId)
            .max(Long::compareTo)
            .ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettTaskForDato(vedtaksdato, nesteId)));

    }

    private List<BehandlingVedtak> finnAlleVedtakForDato(LocalDate vedtaksdato, Long fraVedtakId) {
        var sql ="""
            select * from (select bv.* from BEHANDLING_VEDTAK bv
            join BEHANDLING_RESULTAT br on br.id = bv.BEHANDLING_RESULTAT_ID
            join BEHANDLING b on b.id = br.BEHANDLING_ID
            where b.BEHANDLING_TYPE in ('BT-002','BT-004') and trunc(bv.OPPRETTET_TID) =:vedtaksdato
            and bv.id >:fraVedtakId
            order by bv.id)
            fetch first 100 rows only
            """;

        var query = entityManager.createNativeQuery(sql, BehandlingVedtak.class)
            .setParameter("vedtaksdato", vedtaksdato)
            .setParameter("fraVedtakId", fraVedtakId == null ? 0 : fraVedtakId)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultList();
    }

    private StønadsstatistikkVedtak lagDto(BehandlingVedtak vedtak) {
        var behandlingId = vedtak.getBehandlingsresultat().getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        return lagDto(behandling, vedtak.getId());
    }

    private StønadsstatistikkVedtak lagDto(Behandling behandling, Long vedtakId) {
        LOG.info("Produserer stønadsstatistikk for sak {} behandling {} vedtak {}", behandling.getFagsak().getSaksnummer().getVerdi(), behandling.getId(),
            vedtakId);
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

    private static ProsessTaskData opprettTaskForDato(LocalDate dato, Long fraVedtakId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(StønadsstatistikkMigreringTask.class);

        prosessTaskData.setProperty(StønadsstatistikkMigreringTask.DATO_KEY, dato.toString());
        prosessTaskData.setProperty(StønadsstatistikkMigreringTask.FRA_VEDTAK_ID, String.valueOf(fraVedtakId));
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(150);
        return prosessTaskData;
    }
}
