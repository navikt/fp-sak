package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.SatsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.SatsReguleringRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

@Dependent
@ProsessTask(value = "behandlingsprosess.gregulering.opprett", prioritet = 2)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class GrunnbeløpFinnSakerTask implements ProsessTaskHandler {

    static final String YTELSE_KEY = "ytelse";
    static final String FORRIGE_KEY = "fjor"; // Ta med fjoråret dersom det ikke ble kjørt oppsamlingsheat i fjor
    static final String REVURDERING_KEY = "revurdering"; // Skal revurderinger opprettes?

    private enum Ytelse { FP, SVP }

    private static final Logger LOG = LoggerFactory.getLogger(GrunnbeløpFinnSakerTask.class);

    private final SatsReguleringRepository satsReguleringRepository;
    private final ProsessTaskTjeneste taskTjeneste;
    private final SatsRepository satsRepository;

    @Inject
    public GrunnbeløpFinnSakerTask(SatsReguleringRepository satsReguleringRepository,
                                   ProsessTaskTjeneste taskTjeneste,
                                   SatsRepository satsRepository) {
        this.satsReguleringRepository = satsReguleringRepository;
        this.taskTjeneste = taskTjeneste;
        this.satsRepository = satsRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var ytelse = Optional.ofNullable(prosessTaskData.getPropertyValue(YTELSE_KEY))
            .filter(y -> Arrays.stream(Ytelse.values()).map(Ytelse::name).collect(Collectors.toSet()).contains(y))
            .map(Ytelse::valueOf).orElse(null);
        if (ytelse == null) {
            LOG.warn("GrunnbeløpRegulering ukjent ytelse {}", ytelse);
            return;
        }
        boolean revurder = Optional.ofNullable(prosessTaskData.getPropertyValue(REVURDERING_KEY)).map(Boolean::parseBoolean).orElse(false);
        boolean fjor = Optional.ofNullable(prosessTaskData.getPropertyValue(FORRIGE_KEY)).map(Boolean::parseBoolean).orElse(false);
        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        var callIdRoot = MDCOperations.getCallId();

        var gFomDato = finnGjeldendeGrunnbeløpFomDato(fjor);
        if (gFomDato == null) {
            return;
        }
        var kandidater = switch (ytelse) {
            case FP -> satsReguleringRepository.finnFpSakerMedBehovForGrunnbeløpReguleringKobling(gFomDato);
            case SVP -> satsReguleringRepository.finnSvpSakerMedBehovForGrunnbeløpReguleringKobling(gFomDato);
        };

        if (revurder) {
            kandidater.forEach(sak -> opprettReguleringTask(sak.fagsakId(), sak.saksnummer(), callIdRoot));
        }
        LOG.info("GrunnbeløpRegulering ytelse {} finner {} saker til vurdering", ytelse, kandidater.size());
    }

    private LocalDate finnGjeldendeGrunnbeløpFomDato(boolean fjor) {
        var gjeldende = satsRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now());
        var forrige = satsRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP,
            gjeldende.getPeriode().getFomDato().minusDays(1));
        if (gjeldende.getVerdi() == forrige.getVerdi()) {
            LOG.warn("GrunnbeløpRegulering Samme sats i periodene: gammel {} ny {}", forrige, gjeldende);
            return null;
        }
        return fjor ? forrige.getPeriode().getFomDato() : gjeldende.getPeriode().getFomDato();
    }

    private void opprettReguleringTask(Long fagsakId, Saksnummer saksnummer, String callId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(GrunnbeløpReguleringTask.class);
        prosessTaskData.setFagsak(saksnummer.getVerdi(), fagsakId);
        prosessTaskData.setCallId(callId + "_" + fagsakId);
        taskTjeneste.lagre(prosessTaskData);
    }
}
