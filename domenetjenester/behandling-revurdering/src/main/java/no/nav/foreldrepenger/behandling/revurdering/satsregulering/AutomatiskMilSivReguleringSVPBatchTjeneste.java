package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import java.time.LocalDate;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

/**
 * Batchservice som finner alle behandlinger som skal gjenopptas, og lager en
 * ditto prosess task for hver. Kriterier for gjenopptagelse: Behandlingen har
 * et åpent aksjonspunkt som er et autopunkt og har en frist som er passert.
 */
@ApplicationScoped
public class AutomatiskMilSivReguleringSVPBatchTjeneste implements BatchTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(AutomatiskMilSivReguleringSVPBatchTjeneste.class);
    static final String BATCHNAME = "BVL076";
    private static final String EXECUTION_ID_SEPARATOR = "-";

    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;

    @Inject
    public AutomatiskMilSivReguleringSVPBatchTjeneste(BehandlingRevurderingRepository behandlingRevurderingRepository,
                                                      BeregningsresultatRepository beregningsresultatRepository,
                                                      ProsessTaskTjeneste taskTjeneste) {
        this.behandlingRevurderingRepository = behandlingRevurderingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.taskTjeneste = taskTjeneste;
    }

    @Override
    public AutomatiskGrunnbelopReguleringBatchArguments createArguments(Map<String, String> jobArguments) {
        return new AutomatiskGrunnbelopReguleringBatchArguments(jobArguments);
    }

    @Override
    public String launch(BatchArguments arguments) {
        var opprettRevurdering = (AutomatiskGrunnbelopReguleringBatchArguments) arguments;
        var executionId = BATCHNAME + EXECUTION_ID_SEPARATOR;
        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        final var callId = MDCOperations.getCallId() + "_";
        var gjeldende = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now());
        var forrige = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP,
                gjeldende.getPeriode().getFomDato().minusDays(1));
        if (gjeldende.getVerdi() == forrige.getVerdi()) {
            throw new IllegalArgumentException("Samme sats i periodene: gammel {} ny {}" + forrige + " ny " + gjeldende);
        }
        var tilVurdering = behandlingRevurderingRepository
            .finnSakerMedBehovForMilSivReguleringSVP(gjeldende.getPeriode().getFomDato());
        if ((opprettRevurdering != null) && opprettRevurdering.getSkalRevurdere()) {
            tilVurdering.forEach(sak -> opprettReguleringTask(sak.fagsakId(), sak.aktørId(), callId));
        } else {
            tilVurdering.forEach(sak -> LOG.info("Skal revurdere sak {}", sak.fagsakId()));
        }
        return executionId + tilVurdering.size();
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }

    private void opprettReguleringTask(Long fagsakId, AktørId aktørId, String callId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskGrunnbelopReguleringTask.class);
        prosessTaskData.setFagsak(fagsakId, aktørId.getId());
        prosessTaskData.setCallId(callId + fagsakId);
        prosessTaskData.setPrioritet(100);
        taskTjeneste.lagre(prosessTaskData);
    }
}
