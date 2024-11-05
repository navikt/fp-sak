package no.nav.foreldrepenger.domene.fpinntektsmelding;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("fpinntektsmelding.foresporsel")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class FpinntektsmeldingTask extends GenerellProsessTask {
    public static final String ARBEIDSGIVER_KEY = "arbeidsgiverIdent";
    private static final Logger LOG = LoggerFactory.getLogger(FpinntektsmeldingTask.class);

    private BehandlingRepository behandlingRepository;
    private FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste;

    FpinntektsmeldingTask() {
        // for CDI proxy
    }

    @Inject
    public FpinntektsmeldingTask(BehandlingRepository behandlingRepository,
                                 FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.fpInntektsmeldingTjeneste = fpInntektsmeldingTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var arbeidsgiverIdent = prosessTaskData.getPropertyValue(ARBEIDSGIVER_KEY);
        LOG.info("Starter task for å opprette forespørsel i fpinntektsmelding for behandlingId {} med orgnummer {}",  behandlingId, tilMaskertNummer(arbeidsgiverIdent));
        var ref = BehandlingReferanse.fra(behandling);
        fpInntektsmeldingTjeneste.lagForespørsel(arbeidsgiverIdent, ref);
    }
}
