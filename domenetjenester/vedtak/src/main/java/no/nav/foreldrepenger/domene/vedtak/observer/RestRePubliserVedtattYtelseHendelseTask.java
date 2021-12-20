package no.nav.foreldrepenger.domene.vedtak.observer;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Validation;
import javax.validation.Validator;

import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.abakus.AbakusTjeneste;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("vedtak.restPubliserHendelse")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class RestRePubliserVedtattYtelseHendelseTask implements ProsessTaskHandler {

    public static final String KEY = "vedtattBehandlingId";
    private static final LocalDate Y2020 = LocalDate.of(2020,12,31);

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository tilkjentYtelseRepository;
    private VedtattYtelseTjeneste vedtakTjeneste;
    private AbakusTjeneste abakusTjeneste;
    private Validator validator;

    RestRePubliserVedtattYtelseHendelseTask() {
        // for CDI proxy
    }

    @Inject
    public RestRePubliserVedtattYtelseHendelseTask(BehandlingRepositoryProvider repositoryProvider,
                                                   VedtattYtelseTjeneste vedtakTjeneste,
                                                   AbakusTjeneste abakusTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.tilkjentYtelseRepository = repositoryProvider.getBeregningsresultatRepository();
        this.vedtakTjeneste = vedtakTjeneste;
        this.abakusTjeneste = abakusTjeneste;

        @SuppressWarnings("resource") var factory = Validation.buildDefaultValidatorFactory();
        // hibernate validator implementations er thread-safe, trenger ikke close
        validator = factory.getValidator();
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Optional.ofNullable(prosessTaskData.getPropertyValue(KEY))
            .map(Long::parseLong)
            .flatMap(behandlingRepository::finnUnikBehandlingForBehandlingId)
            .filter(this::harUtbetaling2021)
            .ifPresent(b -> {
                final var ytelse = generatePayload(b);
                try {
                    abakusTjeneste.lagreYtelse(ytelse);
                } catch (IOException e) {
                    throw new TekniskException("FP-19049g", "Kunne ikke lagre vedtak.", e);
                }

            });
    }

    private Ytelse generatePayload(Behandling behandling) {
        var ytelse = vedtakTjeneste.genererYtelse(behandling, true);

        var violations = validator.validate(ytelse);
        if (!violations.isEmpty()) {
            // Har feilet validering
            var allErrors = violations
                .stream()
                .map(it -> it.getPropertyPath().toString() + " :: " + it.getMessage())
                .collect(Collectors.toList());
            throw new IllegalArgumentException("Vedtatt-ytelse valideringsfeil \n " + allErrors);
        }
        return ytelse;
    }

    private boolean harUtbetaling2021(Behandling behandling) {
        return tilkjentYtelseRepository.hentUtbetBeregningsresultat(behandling.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
            .filter(p -> p.getDagsats() > 0)
            .anyMatch(p -> p.getBeregningsresultatPeriodeTom().isAfter(Y2020));
    }

}
