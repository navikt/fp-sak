package no.nav.foreldrepenger.domene.fpinntektsmelding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("fpinntektsmelding.foresporsel")
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class FpinntektsmeldingTask extends GenerellProsessTask {
    private static final Logger LOG = LoggerFactory.getLogger(FpinntektsmeldingTask.class);

    private BehandlingRepository behandlingRepository;
    private FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    FpinntektsmeldingTask() {
        // for CDI proxy
    }

    @Inject
    public FpinntektsmeldingTask(BehandlingRepository behandlingRepository,
                                 FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste,
                                 SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.fpInntektsmeldingTjeneste = fpInntektsmeldingTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);
        LOG.info("Starter task for å opprette forespørsel i fpinntektsmelding for behandlingId {} med skjæringstidspunkt {}",  behandlingId, stp);
        fpInntektsmeldingTjeneste.lagForespørsel(ref, stp);
    }
}
