package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.domene.risikoklassifisering.task.RisikoklassifiseringUtførTask;
import no.nav.foreldrepenger.kontrakter.risk.v1.HentRisikovurderingDto;
import no.nav.foreldrepenger.kontrakter.risk.v1.LagreFaresignalVurderingDto;

import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.domene.risikoklassifisering.mapper.KontrollresultatMapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalWrapper;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikovurderingRequestDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class RisikovurderingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(RisikovurderingTjeneste.class);

    private FpriskTjeneste fpriskTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    public RisikovurderingTjeneste() {
        // CDI
    }

    @Inject
    public RisikovurderingTjeneste(FpriskTjeneste fpriskTjeneste, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.fpriskTjeneste = fpriskTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    private boolean behandlingHarBlittRisikoklassifisert(BehandlingReferanse referanse) {
        var resultat = hentFaresignalerFraFprisk(referanse);
        return resultat.map(res -> !res.kontrollresultat().equals(Kontrollresultat.IKKE_KLASSIFISERT)).orElse(false);
    }

    public Optional<FaresignalWrapper> hentRisikoklassifisering(BehandlingReferanse referanse) {
        // Tidlig return for å spare oss unødige restkall, kun førstegangsbehandlinger blir klassifisert.
        if (!referanse.getBehandlingType().equals(BehandlingType.FØRSTEGANGSSØKNAD)) {
            return Optional.empty();
        }
        return hentFaresignalerFraFprisk(referanse);
    }

    public boolean skalVurdereFaresignaler(BehandlingReferanse referanse) {
        Objects.requireNonNull(referanse, "referanse");
        var wrapper = hentRisikoklassifisering(referanse);
        return wrapper.map(this::erHøyRisiko).orElse(false);
    }

    public void lagreVurderingAvFaresignalerForBehandling(BehandlingReferanse referanse, FaresignalVurdering vurdering) {
        Objects.requireNonNull(referanse, "referanse");
        // Send svar til fprisk
        sendVurderingTilFprisk(referanse, vurdering);
    }

    public void lagreProsesstaskForRisikoklassifisering(BehandlingReferanse referanse) {
        if (behandlingHarBlittRisikoklassifisert(referanse)) {
            LOG.info("Risikoklassifisering er allerede blitt utført på behandling " + referanse.getBehandlingId());
        } else {
            LOG.info("Oppretter task for risikoklassifisering på behandling " + referanse.getBehandlingId());
            var callId = MDCOperations.getCallId();
            if (callId == null || callId.isBlank())
                callId = MDCOperations.generateCallId();
            var taskData = ProsessTaskData.forProsessTask(RisikoklassifiseringUtførTask.class);
            taskData.setBehandling(referanse.getFagsakId(), referanse.getBehandlingId(), referanse.getAktørId().getId());
            taskData.setCallId(callId);
            prosessTaskTjeneste.lagre(taskData);
        }
    }

    public void startRisikoklassifisering(BehandlingReferanse referanse, RisikovurderingRequestDto request) {
        if (behandlingHarBlittRisikoklassifisert(referanse)) {
            LOG.info("Risikoklassifisering er allerede blitt utført på behandling " + referanse.getBehandlingId());
        } else {
            LOG.info("Iverksetter risikoklassifisering på behandling " + referanse.getBehandlingId());
            fpriskTjeneste.sendRisikoklassifiseringsoppdrag(request);
        }
    }

    private void sendVurderingTilFprisk(BehandlingReferanse referanse, FaresignalVurdering vurdering) {
        var request = new LagreFaresignalVurderingDto(referanse.getBehandlingUuid(), KontrollresultatMapper.mapFaresignalvurderingTilKontrakt(vurdering));
        fpriskTjeneste.sendRisikovurderingTilFprisk(request);
    }

    private boolean erHøyRisiko(FaresignalWrapper wrapper) {
        return Objects.equals(wrapper.kontrollresultat(), Kontrollresultat.HØY);
    }

    private Optional<FaresignalWrapper> hentFaresignalerFraFprisk(BehandlingReferanse ref) {
        var request = new HentRisikovurderingDto(ref.getBehandlingUuid());
        var faresignalerRespons = fpriskTjeneste.hentFaresignalerForBehandling(request);
        if (faresignalerRespons.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(KontrollresultatMapper.fraFaresignalRespons(faresignalerRespons.get()));
    }
}
