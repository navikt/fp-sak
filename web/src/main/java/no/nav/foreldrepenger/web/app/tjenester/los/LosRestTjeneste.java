package no.nav.foreldrepenger.web.app.tjenester.los;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling.NøkkeltallBehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling.NøkkeltallBehandlingVentestatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.hendelser.behandling.los.LosBehandlingDto;
import no.nav.vedtak.hendelser.behandling.los.LosFagsakEgenskaperDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

import java.util.List;

@ApplicationScoped
@Transactional
@Path(LosRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class LosRestTjeneste {

    static final String BASE_PATH = "/los";

    private static final String LOS_BEHANDLING_PATH = "/los-behandling";
    private static final String LOS_FAGSAK_EGENSKAP_PATH = "/los-egenskap";
    public static final String LOS_NØKKELTALL_PATH = "/los-nokkeltall";

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private LosBehandlingDtoTjeneste losBehandlingDtoTjeneste;
    private NøkkeltallBehandlingRepository nøkkeltallBehandlingRepository;

    public LosRestTjeneste() {
        // CDI
    }

    @Inject
    public LosRestTjeneste(BehandlingRepository behandlingRepository,
                           FagsakRepository fagsakRepository,
                           LosBehandlingDtoTjeneste losBehandlingDtoTjeneste,
                           NøkkeltallBehandlingRepository nøkkeltallBehandlingRepository) {
        this.losBehandlingDtoTjeneste = losBehandlingDtoTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.nøkkeltallBehandlingRepository = nøkkeltallBehandlingRepository;
    }

    @GET
    @Path(LOS_BEHANDLING_PATH)
    @Operation(description = "Hent behandling gitt id for LOS", summary = "Returnerer behandlingen som er tilknyttet uuid.", tags = "los-data", responses = {@ApiResponse(responseCode = "200", description = "Returnerer behandling", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = LosBehandlingDto.class))}),})
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

    @GET
    @Path(LOS_FAGSAK_EGENSKAP_PATH)
    @Operation(description = "Hent egenskaper for fagsak gitt saksnummer for LOS", summary = "Returnerer saksegenskaper som er tilknyttet saksnummer.", tags = "los-data", responses = {@ApiResponse(responseCode = "200", description = "Returnerer behandling", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = LosFagsakEgenskaperDto.class))}),})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Response hentFagsakEgenskaper(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class) @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto s) {
        return fagsakRepository.hentSakGittSaksnummer(new Saksnummer(s.getVerdi()))
            .map(f -> losBehandlingDtoTjeneste.lagFagsakEgenskaper(f))
            .map(Response::ok)
            .orElseGet(() -> Response.status(Response.Status.FORBIDDEN)).build(); // Etablert praksis
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
