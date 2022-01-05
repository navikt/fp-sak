package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.risikoklassifisering.task.MigrerFaresignalvurderingTask;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.function.Function;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;

@Path("/forvaltningRisk")
@ApplicationScoped
@Transactional
public class ForvaltningRiskRestTjeneste {
    private RisikovurderingTjeneste risikovurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste taskTjeneste;

    public ForvaltningRiskRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningRiskRestTjeneste(RisikovurderingTjeneste risikovurderingTjeneste,
                                       BehandlingRepository behandlingRepository,
                                       ProsessTaskTjeneste taskTjeneste) {
        this.risikovurderingTjeneste = risikovurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.taskTjeneste = taskTjeneste;
    }

    @POST
    @Path("/migrer-alle")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Lager en task for hver behandling som skal migrere sin faresignalvurdering til fprisk", tags = "FORVALTNING-risk", responses = {
            @ApiResponse(responseCode = "200", description = "Tasker er opprettet.")
    })
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public Response migrerEnVurdering(){
        var alleSakerSomMåMigreres = risikovurderingTjeneste.finnBehandlingIderForMigrering();
        var spread = 1800; // Sprer kjøring over 30 minutter
        var baseline = LocalDateTime.now();
        alleSakerSomMåMigreres.forEach(behandlingId -> lagreTask(behandlingId, baseline.plusSeconds(LocalDateTime.now().getNano() % spread)));
        return Response.ok().build();
    }

    private void lagreTask(Long behandlingId, LocalDateTime kjøreStart) {
        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        var callId = MDCOperations.getCallId();
        var prosessTaskData = ProsessTaskData.forProsessTask(MigrerFaresignalvurderingTask.class);
        prosessTaskData.setProperty(MigrerFaresignalvurderingTask.BEHANDLING_ID, behandlingId.toString());
        prosessTaskData.setNesteKjøringEtter(kjøreStart);
        prosessTaskData.setCallId(callId);
        prosessTaskData.setPrioritet(50);
        taskTjeneste.lagre(prosessTaskData);
    }

    @POST
    @Path("/migrer-en")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Lager en task for for å migrere vurdering av faresignaler for en enkelt behandling til fprisk", tags = "FORVALTNING-risk", responses = {
        @ApiResponse(responseCode = "200", description = "Tasken er opprettet.")
    })
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public Response migrerEnVurdering(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
    @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandlingId = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid()).getId();
        lagreTask(behandlingId, LocalDateTime.now());
        return Response.ok().build();
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }

}
