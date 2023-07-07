package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.SatsReguleringRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
@ProsessTask("behandlingsprosess.esregulering.opprett")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class EngangsstønadFinnSakerTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(EngangsstønadFinnSakerTask.class);

    static final String REVURDERING_KEY = "revurdering"; // Skal revurderinger opprettes?

    private LegacyESBeregningRepository esBeregningRepository;
    private SatsReguleringRepository satsReguleringRepository;
    private ProsessTaskTjeneste taskTjeneste;

    EngangsstønadFinnSakerTask() {
        // for CDI proxy
    }

    @Inject
    public EngangsstønadFinnSakerTask(SatsReguleringRepository satsReguleringRepository,
                                      LegacyESBeregningRepository esBeregningRepository,
                                      ProsessTaskTjeneste taskTjeneste) {
        this.satsReguleringRepository = satsReguleringRepository;
        this.esBeregningRepository = esBeregningRepository;
        this.taskTjeneste = taskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        boolean revurder = Optional.ofNullable(prosessTaskData.getPropertyValue(REVURDERING_KEY)).map(Boolean::parseBoolean).orElse(false);
        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        var callIdRoot = MDCOperations.getCallId() + "_";

        var esFomDato = finnGjeldendeSatsFomDato();
        if (esFomDato == null) {
            return;
        }
        var kandidater = satsReguleringRepository.finnSakerMedBehovForEngangsstønadRegulering(esFomDato);

        if (revurder) {
            kandidater.forEach(sak -> opprettReguleringTask(sak.fagsakId(), sak.aktørId(), callIdRoot));
        }
        LOG.info("ESregulering finner {} saker til vurdering", kandidater.size());
    }

    private LocalDate finnGjeldendeSatsFomDato() {
        var gjeldende = esBeregningRepository.finnEksaktSats(BeregningSatsType.ENGANG, LocalDate.now());
        var forrige = esBeregningRepository.finnEksaktSats(BeregningSatsType.ENGANG, gjeldende.getPeriode().getFomDato().minusDays(1));
        if (gjeldende.getVerdi() == forrige.getVerdi()) {
            LOG.warn("ESregulering Samme sats i periodene: gammel {} ny {}", forrige, gjeldende);
            return null;
        }
        return gjeldende.getPeriode().getFomDato();
    }

    private void opprettReguleringTask(Long fagsakId, AktørId aktørId, String callId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(EngangsstønadReguleringTask.class);
        prosessTaskData.setFagsak(fagsakId, aktørId.getId());
        prosessTaskData.setCallId(callId + fagsakId);
        prosessTaskData.setPrioritet(100);
        taskTjeneste.lagre(prosessTaskData);
    }

}
