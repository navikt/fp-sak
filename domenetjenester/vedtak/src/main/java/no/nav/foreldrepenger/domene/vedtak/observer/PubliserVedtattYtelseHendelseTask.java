package no.nav.foreldrepenger.domene.vedtak.observer;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "vedtak.publiserHendelse", prioritet = 2)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class PubliserVedtattYtelseHendelseTask implements ProsessTaskHandler {

    public static final String KEY = "vedtattBehandlingId";

    private BehandlingRepository behandlingRepository;
    private VedtattYtelseTjeneste vedtakTjeneste;
    private Validator validator;
    private VedtakHendelseKafkaProducer producer;

    PubliserVedtattYtelseHendelseTask() {
        // for CDI proxy
    }

    @Inject
    public PubliserVedtattYtelseHendelseTask(BehandlingRepositoryProvider repositoryProvider,
                                             VedtattYtelseTjeneste vedtakTjeneste,
                                             VedtakHendelseKafkaProducer producer) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vedtakTjeneste = vedtakTjeneste;
        this.producer = producer;

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
                var payload = generatePayload(b);
                producer.sendJson(b.getSaksnummer().getVerdi(), payload);
            });
    }

    private String generatePayload(Behandling behandling) {
        var ytelse = vedtakTjeneste.genererYtelse(behandling);

        var violations = validator.validate(ytelse);
        if (!violations.isEmpty()) {
            // Har feilet validering
            var allErrors = violations.stream().map(it -> it.getPropertyPath().toString() + " :: " + it.getMessage()).toList();
            throw new IllegalArgumentException("Vedtatt-ytelse valideringsfeil \n " + allErrors);
        }
        return toJson(ytelse);
    }

    private String toJson(Ytelse ytelse) {
        return StandardJsonConfig.toJson(ytelse);
    }
}
