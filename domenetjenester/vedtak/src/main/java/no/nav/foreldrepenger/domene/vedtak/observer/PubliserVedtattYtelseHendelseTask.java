package no.nav.foreldrepenger.domene.vedtak.observer;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.folketrygdloven.kalkulator.JsonMapper;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

import java.util.Optional;

@ApplicationScoped
@ProsessTask("vedtak.publiserHendelse")
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
                producer.sendJson(b.getFagsak().getSaksnummer().getVerdi(), payload);
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
        try {
            return JsonMapper.toJson(ytelse);
        } catch (JsonProcessingException e) {
            throw new TekniskException("FP-190495", "Kunne ikke serialisere til json.", e);
        }
    }
}
