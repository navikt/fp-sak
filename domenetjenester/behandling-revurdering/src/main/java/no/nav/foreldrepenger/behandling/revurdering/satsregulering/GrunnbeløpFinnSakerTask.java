package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.SatsReguleringRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.typer.AktørId;
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
    static final String REGULERING_TYPE_KEY = "regtype";
    static final String ARENA_SATSFOM_KEY = "satsfomdato"; // Optional - brukes bare for ny sats utenom årlig regulering
    static final String ARENA_JUSTERT_KEY = "justertdato"; // Mandag etter foretatt regulering av AAP og Dagpenger
    static final String REVURDERING_KEY = "revurdering"; // Skal revurderinger opprettes?

    private enum Ytelse { FP, SVP }
    private enum ReguleringType { G6, MS, SN, ARENA }

    private static final Logger LOG = LoggerFactory.getLogger(GrunnbeløpFinnSakerTask.class);

    private static final Map<ReguleringType, Supplier<Long>> MULTIPLIKATORER = Map.of(
        ReguleringType.G6, BeregningsresultatRepository::avkortingMultiplikatorG,
        ReguleringType.MS, BeregningsresultatRepository::militærMultiplikatorG,
        ReguleringType.SN, () -> 0L
    );

    private final SatsReguleringRepository satsReguleringRepository;
    private final BeregningsresultatRepository beregningsresultatRepository;
    private final ProsessTaskTjeneste taskTjeneste;

    @Inject
    public GrunnbeløpFinnSakerTask(SatsReguleringRepository satsReguleringRepository,
                                   BeregningsresultatRepository beregningsresultatRepository,
                                   ProsessTaskTjeneste taskTjeneste) {
        this.satsReguleringRepository = satsReguleringRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.taskTjeneste = taskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var ytelse = Optional.ofNullable(prosessTaskData.getPropertyValue(YTELSE_KEY))
            .filter(y -> Arrays.stream(Ytelse.values()).map(Ytelse::name).collect(Collectors.toSet()).contains(y))
            .map(Ytelse::valueOf).orElse(null);
        var reguleringType = Optional.ofNullable(prosessTaskData.getPropertyValue(REGULERING_TYPE_KEY))
            .filter(a -> Arrays.stream(ReguleringType.values()).map(ReguleringType::name).collect(Collectors.toSet()).contains(a))
            .map(ReguleringType::valueOf).orElse(null);
        if (ytelse == null || reguleringType == null || Ytelse.SVP.equals(ytelse) && ReguleringType.ARENA.equals(reguleringType)) {
            LOG.warn("GrunnbeløpRegulering ukjent ytelse {} eller reguleringtype {}", ytelse, reguleringType);
            return;
        }
        boolean revurder = Optional.ofNullable(prosessTaskData.getPropertyValue(REVURDERING_KEY)).map(Boolean::parseBoolean).orElse(false);
        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        var callIdRoot = MDCOperations.getCallId() + "_";

        var gFomDato = finnGjeldendeGrunnbeløpFomDato();
        if (gFomDato == null) {
            return;
        }
        var kandidater = ReguleringType.ARENA.equals(reguleringType) ? finnArenaKandidater(prosessTaskData, gFomDato) : finnKandidater(ytelse, reguleringType, gFomDato);

        if (revurder) {
            kandidater.forEach(sak -> opprettReguleringTask(sak.fagsakId(), sak.aktørId(), callIdRoot));
        }
        LOG.info("GrunnbeløpRegulering ytelse {} type {} finner {} saker til vurdering", ytelse, reguleringType, kandidater.size());
    }

    private LocalDate finnGjeldendeGrunnbeløpFomDato() {
        var gjeldende = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now());
        var forrige = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP,
            gjeldende.getPeriode().getFomDato().minusDays(1));
        if (gjeldende.getVerdi() == forrige.getVerdi()) {
            LOG.warn("GrunnbeløpRegulering Samme sats i periodene: gammel {} ny {}", forrige, gjeldende);
            return null;
        }
        return gjeldende.getPeriode().getFomDato();
    }

    private List<SatsReguleringRepository.FagsakIdAktørId> finnArenaKandidater(ProsessTaskData prosessTaskData, LocalDate gFomDato) {
        var satsdato = Optional.ofNullable(prosessTaskData.getPropertyValue(ARENA_SATSFOM_KEY))
            .map(LocalDate::parse).orElse(gFomDato);
        var arenadato = Optional.ofNullable(prosessTaskData.getPropertyValue(ARENA_JUSTERT_KEY))
            .map(LocalDate::parse).orElse(null);
        if (arenadato == null || !satsdato.isBefore(arenadato)) {
            LOG.warn("GrunnbeløpRegulering ugyldig Arena-dato {} for satsdato {}", arenadato, satsdato);
            return List.of();
        }
        return satsReguleringRepository.finnSakerMedBehovForArenaRegulering(satsdato, arenadato);
    }

    private List<SatsReguleringRepository.FagsakIdAktørId> finnKandidater(Ytelse ytelse, ReguleringType regType, LocalDate gFomDato) {
        var multiplikator = Optional.ofNullable(MULTIPLIKATORER.get(regType)).map(Supplier::get)
            .orElseThrow(() -> new IllegalArgumentException("Logisk brist for ytelse " + ytelse.name() + " reg.type " + regType.name()));
        return switch (regType) {
            case G6 -> Ytelse.FP.equals(ytelse) ?
                satsReguleringRepository.finnSakerMedBehovForGrunnbeløpRegulering(gFomDato, multiplikator) :
                satsReguleringRepository.finnSakerMedBehovForGrunnbeløpReguleringSVP(gFomDato, multiplikator);
            case MS -> Ytelse.FP.equals(ytelse) ?
                satsReguleringRepository.finnSakerMedBehovForMilSivRegulering(gFomDato, multiplikator) :
                satsReguleringRepository.finnSakerMedBehovForMilSivReguleringSVP(gFomDato, multiplikator);
            case SN -> Ytelse.FP.equals(ytelse) ?
                satsReguleringRepository.finnSakerMedBehovForNæringsdrivendeRegulering(gFomDato, multiplikator) :
                satsReguleringRepository.finnSakerMedBehovForNæringsdrivendeReguleringSVP(gFomDato, multiplikator);
            default -> throw new IllegalArgumentException("Logisk brist for ytelse " + ytelse.name() + " reg.type " + regType.name());
        };
    }

    private void opprettReguleringTask(Long fagsakId, AktørId aktørId, String callId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(GrunnbeløpReguleringTask.class);
        prosessTaskData.setFagsak(fagsakId, aktørId.getId());
        prosessTaskData.setCallId(callId + fagsakId);
        taskTjeneste.lagre(prosessTaskData);
    }
}
