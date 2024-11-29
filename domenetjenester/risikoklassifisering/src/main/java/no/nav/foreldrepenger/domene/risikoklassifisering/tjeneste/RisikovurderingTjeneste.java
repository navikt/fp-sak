package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.domene.risikoklassifisering.mapper.KontrollresultatMapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.task.RisikoklassifiseringUtførTask;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalWrapper;
import no.nav.foreldrepenger.kontrakter.risk.v1.HentRisikovurderingDto;
import no.nav.foreldrepenger.kontrakter.risk.v1.LagreFaresignalVurderingDto;
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
    public RisikovurderingTjeneste(FpriskTjeneste fpriskTjeneste,
                                   ProsessTaskTjeneste prosessTaskTjeneste) {
        this.fpriskTjeneste = fpriskTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }


    public Optional<FaresignalWrapper> hentRisikoklassifisering(BehandlingReferanse referanse) {
        if (referanse.behandlingType().equals(BehandlingType.FØRSTEGANGSSØKNAD)) {
            return hentFaresignalerFraFprisk(referanse);
        }
        return Optional.empty();
    }

    public boolean skalVurdereFaresignaler(BehandlingReferanse referanse) {
        Objects.requireNonNull(referanse, "referanse");
        // Skal kun løse aksjonspunkt for faresignaler i førstegangsbehandling
        if (referanse.behandlingType().equals(BehandlingType.FØRSTEGANGSSØKNAD)) {
            var wrapper = hentRisikoklassifisering(referanse);
            return wrapper.map(this::erHøyRisiko).orElse(false);
        }
        return false;
    }

    public void lagreVurderingAvFaresignalerForBehandling(BehandlingReferanse referanse, FaresignalVurdering vurdering) {
        Objects.requireNonNull(referanse, "referanse");
        sendVurderingTilFprisk(referanse, vurdering);
    }

    public void lagreProsesstaskForRisikoklassifisering(BehandlingReferanse referanse) {
        if (behandlingHarBlittRisikoklassifisert(referanse)) {
            LOG.info("Risikoklassifisering er allerede blitt utført på behandling " + referanse.behandlingId());
        } else {
            LOG.info("Oppretter task for risikoklassifisering på behandling " + referanse.behandlingId());
            var callId = MDCOperations.getCallId();
            if (callId == null || callId.isBlank())
                callId = MDCOperations.generateCallId();
            var taskData = ProsessTaskData.forProsessTask(RisikoklassifiseringUtførTask.class);
            taskData.setBehandling(referanse.saksnummer().getVerdi(), referanse.fagsakId(), referanse.behandlingId());
            taskData.setCallId(callId);
            prosessTaskTjeneste.lagre(taskData);
        }
    }

    public void startRisikoklassifisering(BehandlingReferanse referanse, RisikovurderingRequestDto request) {
        if (behandlingHarBlittRisikoklassifisert(referanse)) {
            LOG.info("Risikoklassifisering er allerede blitt utført på behandling " + referanse.behandlingId());
        } else {
            LOG.info("Iverksetter risikoklassifisering på behandling " + referanse.behandlingId());
            fpriskTjeneste.sendRisikoklassifiseringsoppdrag(request);
        }
    }

    private boolean behandlingHarBlittRisikoklassifisert(BehandlingReferanse referanse) {
        var resultat = hentFaresignalerFraFprisk(referanse);
        return resultat.map(res -> !res.kontrollresultat().equals(Kontrollresultat.IKKE_KLASSIFISERT)).orElse(false);
    }

    private void sendVurderingTilFprisk(BehandlingReferanse referanse, FaresignalVurdering vurdering) {
        var request = new LagreFaresignalVurderingDto(referanse.behandlingUuid(), KontrollresultatMapper.mapFaresignalvurderingTilKontrakt(vurdering));
        fpriskTjeneste.sendRisikovurderingTilFprisk(request);
    }

    private boolean erHøyRisiko(FaresignalWrapper wrapper) {
        return Objects.equals(wrapper.kontrollresultat(), Kontrollresultat.HØY);
    }

    private Optional<FaresignalWrapper> hentFaresignalerFraFprisk(BehandlingReferanse ref) {
        var request = new HentRisikovurderingDto(ref.behandlingUuid());
        return fpriskTjeneste.hentFaresignalerForBehandling(request).map(KontrollresultatMapper::fraFaresignalRespons);
    }
}
