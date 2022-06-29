package no.nav.foreldrepenger.domene.vedtak.observer;

import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Validation;
import javax.validation.Validator;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.folketrygdloven.kalkulator.JsonMapper;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("vedtak.publiserHendelse")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class PubliserVedtattYtelseHendelseTask implements ProsessTaskHandler {

    public static final String KEY = "vedtattBehandlingId";

    private BehandlingRepository behandlingRepository;
    private VedtattYtelseTjeneste vedtakTjeneste;
    private HendelseProducer producer;
    private Validator validator;

    PubliserVedtattYtelseHendelseTask() {
        // for CDI proxy
    }

    @Inject
    public PubliserVedtattYtelseHendelseTask(BehandlingRepositoryProvider repositoryProvider,
                                             VedtattYtelseTjeneste vedtakTjeneste,
                                             @KonfigVerdi("kafka.fattevedtak.topic") String topicName,
                                             @KonfigVerdi("kafka.bootstrap.servers") String bootstrapServers,
                                             @KonfigVerdi("kafka.schema.registry.url") String schemaRegistryUrl,
                                             @KonfigVerdi("systembruker.username") String username,
                                             @KonfigVerdi("systembruker.password") String password) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vedtakTjeneste = vedtakTjeneste;
        this.producer = new HendelseProducer(topicName, bootstrapServers, schemaRegistryUrl, username, password);

        @SuppressWarnings("resource") var factory = Validation.buildDefaultValidatorFactory();
        // hibernate validator implementations er thread-safe, trenger ikke close
        validator = factory.getValidator();
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Optional.ofNullable(prosessTaskData.getPropertyValue(KEY))
            .filter(s -> !s.isEmpty())
            .map(Long::parseLong)
            .flatMap(behandlingRepository::finnUnikBehandlingForBehandlingId)
            .ifPresent(b -> {
                final var payload = generatePayload(b);
                producer.sendJson(payload);
            });
    }

    private String generatePayload(Behandling behandling) {
        var ytelse = vedtakTjeneste.genererYtelse(behandling, true);

        var violations = validator.validate(ytelse);
        if (!violations.isEmpty()) {
            // Har feilet validering
            var allErrors = violations.stream().map(it -> it.getPropertyPath().toString() + " :: " + it.getMessage()).toList();
            throw new IllegalArgumentException("Vedtatt-ytelse valideringsfeil \n " + allErrors);
        }
        return toJson(ytelse);
    }

    private String toJson(Ytelse ytelse) {
        try {
            return JsonMapper.toJson(ytelse);
        } catch (JsonProcessingException e) {
            throw new TekniskException("FP-190495", "Kunne ikke serialisere til json.", e);
        }
    }
}
