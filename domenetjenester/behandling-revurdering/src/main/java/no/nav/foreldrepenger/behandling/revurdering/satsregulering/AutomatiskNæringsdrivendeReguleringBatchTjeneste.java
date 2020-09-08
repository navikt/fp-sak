package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchStatus;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.util.Tuple;

/**
 *  Batchservice som finner alle behandlinger som skal gjenopptas, og lager en ditto prosess task for hver.
 *  Kriterier for gjenopptagelse: Behandling for Selvstendig Næringsdrivende, evt i kombinasjon med arbeid el frilans
 */
@ApplicationScoped
public class AutomatiskNæringsdrivendeReguleringBatchTjeneste implements BatchTjeneste {
    private static final Logger log = LoggerFactory.getLogger(AutomatiskNæringsdrivendeReguleringBatchTjeneste.class);
    static final String BATCHNAME = "BVL074";
    private static final String EXECUTION_ID_SEPARATOR = "-";

    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    @Inject
    public AutomatiskNæringsdrivendeReguleringBatchTjeneste(BehandlingRevurderingRepository behandlingRevurderingRepository,
                                                            BeregningsresultatRepository beregningsresultatRepository,
                                                            ProsessTaskRepository prosessTaskRepository) {
        this.behandlingRevurderingRepository = behandlingRevurderingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public AutomatiskGrunnbelopReguleringBatchArguments createArguments(Map<String, String> jobArguments) {
        return new AutomatiskGrunnbelopReguleringBatchArguments(jobArguments);
    }

    @Override
    public String launch(BatchArguments arguments) {
        AutomatiskGrunnbelopReguleringBatchArguments opprettRevurdering = (AutomatiskGrunnbelopReguleringBatchArguments)arguments;
        String executionId = BATCHNAME + EXECUTION_ID_SEPARATOR;
        final String callId = (MDCOperations.getCallId() == null ? MDCOperations.generateCallId() : MDCOperations.getCallId()) + "_";
        BeregningSats gjeldende = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now());
        BeregningSats forrige = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, gjeldende.getPeriode().getFomDato().minusDays(1));
        if (gjeldende.getVerdi() == forrige.getVerdi()) {
            throw new IllegalArgumentException("Samme sats i periodene: gammel {} ny {}" + forrige + " ny " + gjeldende);
        }
        List<Tuple<Long, AktørId>> tilVurdering = behandlingRevurderingRepository.finnSakerMedBehovForNæringsdrivendeRegulering(forrige.getVerdi(), gjeldende.getPeriode().getFomDato());
        if (opprettRevurdering != null && opprettRevurdering.getSkalRevurdere()) {
            tilVurdering.forEach(sak -> opprettReguleringTask(sak.getElement1(), sak.getElement2(), callId));
        } else {
            tilVurdering.forEach(sak -> log.info("Skal revurdere sak {}", sak.getElement1()));
        }
        return executionId + tilVurdering.size();
    }

    @Override
    public BatchStatus status(String batchInstanceNumber) {
        return BatchStatus.OK;
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }

    private void opprettReguleringTask(Long fagsakId, AktørId aktørId, String callId) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskGrunnbelopReguleringTask.TASKTYPE);
        prosessTaskData.setFagsak(fagsakId, aktørId.getId());
        prosessTaskData.setCallId(callId + fagsakId);
        prosessTaskRepository.lagre(prosessTaskData);
    }
}
