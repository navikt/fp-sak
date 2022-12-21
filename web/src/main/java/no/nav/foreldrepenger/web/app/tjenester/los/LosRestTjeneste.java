package no.nav.foreldrepenger.web.app.tjenester.los;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling.NøkkeltallBehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling.NøkkeltallBehandlingVentestatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.hendelser.behandling.los.LosBehandlingDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@ApplicationScoped
@Transactional
@Path(LosRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class LosRestTjeneste {

    static final String BASE_PATH = "/los";

    private static final String LOS_BEHANDLING_PATH = "/los-behandling";
    public static final String LOS_NØKKELTALL_PATH = "/los-nokkeltall";

    private BehandlingRepository behandlingRepository;
    private LosBehandlingDtoTjeneste losBehandlingDtoTjeneste;
    private NøkkeltallBehandlingRepository nøkkeltallBehandlingRepository;

    public LosRestTjeneste() {
        // CDI
    }

    @Inject
    public LosRestTjeneste(BehandlingRepository behandlingRepository,
                           LosBehandlingDtoTjeneste losBehandlingDtoTjeneste,
                           NøkkeltallBehandlingRepository nøkkeltallBehandlingRepository) {
        this.losBehandlingDtoTjeneste = losBehandlingDtoTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.nøkkeltallBehandlingRepository = nøkkeltallBehandlingRepository;
    }

    @GET
    @Path(LOS_BEHANDLING_PATH)
    @Operation(description = "Hent behandling gitt id for LOS", summary = ("Returnerer behandlingen som er tilknyttet uuid."), tags = "los-data", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer behandling", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = LosBehandlingDto.class))
            }),
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Response hentBehandlingResultatForBackend(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var dto = losBehandlingDtoTjeneste.lagLosBehandlingDto(behandling,
            behandlingRepository.hentSistOppdatertTidspunkt(behandling.getId()).isPresent());
        var responseBuilder = Response.ok().entity(dto);
        return responseBuilder.build();
    }

    @Path(LOS_NØKKELTALL_PATH)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Tilbyr data over ikke-avsluttede behandlinger på vent vs ikke på vent", tags = "los-data")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.OPPGAVESTYRING_AVDELINGENHET, sporingslogg = false)
    public List<NøkkeltallBehandlingVentestatus> innloggetBruker() {
        return nøkkeltallBehandlingRepository.hentNøkkeltallBehandlingVentestatus();
    }

}
