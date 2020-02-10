package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.task.TilbakerullingBeregningTask;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;

@Path("/forvaltningBeregning")
@ApplicationScoped
@Transaction
public class ForvaltningBeregningRestTjeneste {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;

    @Inject
    public ForvaltningBeregningRestTjeneste(
        BeregningsgrunnlagRepository beregningsgrunnlagRepository, BehandlingRepository behandlingRepository,
        ProsessTaskRepository prosessTaskRepository) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public ForvaltningBeregningRestTjeneste() {
        //CDI
    }

    @POST
    @Path("/tilbakerullingAlleSakerBeregning")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Rull saker tilbake til beregning", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(action = READ, ressurs = BeskyttetRessursResourceAttributt.DRIFT, sporingslogg = false)
    public Response tilbakerullAlleSakerBeregning() {
        beregningsgrunnlagRepository.opprettProsesstaskForTilbakerullingAvSakerBeregning();
        return Response.ok().build();
    }

    @POST
    @Path("/tilbakerullingBeregning")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Rull sak tilbake til beregning", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(action = READ, ressurs = BeskyttetRessursResourceAttributt.DRIFT, sporingslogg = false)
    public Response tilbakerullEnSakBeregning(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        Behandling behandling = behandlingRepository.hentBehandling(dto.getBehandlingId());
        opprettTilbakerullingBeregningTask(behandling);
        return Response.ok().build();
    }

    private void opprettTilbakerullingBeregningTask(Behandling behandling) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(TilbakerullingBeregningTask.TASKNAME);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

}
