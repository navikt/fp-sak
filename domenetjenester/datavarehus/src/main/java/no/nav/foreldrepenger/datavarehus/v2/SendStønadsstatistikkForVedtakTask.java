package no.nav.foreldrepenger.datavarehus.v2;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaSender;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.stønadsstatistikk", prioritet = 2)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SendStønadsstatistikkForVedtakTask extends GenerellProsessTask {

    private final StønadsstatistikkTjeneste stønadsstatistikkTjeneste;
    private final BehandlingRepository behandlingRepository;
    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private final StønadsstatistikkKafkaProducer kafkaProducer;
    private final Validator validator;

    @Inject
    public SendStønadsstatistikkForVedtakTask(StønadsstatistikkTjeneste stønadsstatistikkTjeneste,
                                              BehandlingRepository behandlingRepository,
                                              BehandlingsresultatRepository behandlingsresultatRepository,
                                              SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                              StønadsstatistikkKafkaProducer kafkaProducer) {
        this.stønadsstatistikkTjeneste = stønadsstatistikkTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.kafkaProducer = kafkaProducer;
        @SuppressWarnings("resource") var factory = Validation.buildDefaultValidatorFactory();
        // hibernate validator implementations er thread-safe, trenger ikke close
        validator = factory.getValidator();
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (!behandling.erYtelseBehandling() || behandlingsresultatRepository.hent(behandling.getId()).isBehandlingHenlagt()) {
            throw new IllegalStateException("Prøver å sende stønadsstatistikk på en behandling som ikke er ytelsebehandling eller er henlagt");
        }
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var vedtak = stønadsstatistikkTjeneste.genererVedtak(BehandlingReferanse.fra(behandling), stp);

        valider(vedtak);
        var header = new KafkaSender.KafkaHeader("stønadstype", vedtak.getYtelseType().name().getBytes());
        kafkaProducer.sendJson(header, vedtak.getSaksnummer().id(), DefaultJsonMapper.toJson(vedtak));
    }

    private void valider(StønadsstatistikkVedtak vedtak) {
        var violations = validator.validate(vedtak);
        if (!violations.isEmpty()) {
            // Har feilet validering
            var allErrors = violations.stream().map(it -> it.getPropertyPath().toString() + " :: " + it.getMessage()).toList();
            throw new IllegalArgumentException("stønadsstatistikk valideringsfeil \n " + allErrors);
        }
    }
}
