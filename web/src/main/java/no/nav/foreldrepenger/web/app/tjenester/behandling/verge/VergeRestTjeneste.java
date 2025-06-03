package no.nav.foreldrepenger.web.app.tjenester.behandling.verge;

import java.util.Optional;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeBackendDto;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@ApplicationScoped
@Transactional
@Path(VergeRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class VergeRestTjeneste {

    public static final String BASE_PATH = "/verge";
    private static final String VERGE_BACKEND_PART_PATH = "/backend";
    public static final String VERGE_BACKEND_PATH = BASE_PATH + VERGE_BACKEND_PART_PATH;

    private static final String VERGE_FJERN_PART_PATH = "/fjern";
    public static final String VERGE_FJERN_PATH = BASE_PATH + VERGE_FJERN_PART_PATH;

    private static final String VERGE_OPPRETT_PART_PATH = "/opprett";
    public static final String VERGE_OPPRETT_PATH = BASE_PATH + VERGE_OPPRETT_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private VergeTjeneste vergeTjeneste;

    public VergeRestTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    @Inject
    public VergeRestTjeneste(BehandlingRepository behandlingRepository,
                             VergeTjeneste vergeTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.vergeTjeneste = vergeTjeneste;
    }

    @GET
    @Operation(description = "Henter verge/fullmektig på behandlingen", tags = "verge", responses = {@ApiResponse(responseCode = "200", description = "Verge/fullmektig funnet"), @ApiResponse(responseCode = "204", description = "Ingen verge/fullmektig")})
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public VergeDto hentVerge(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class) @QueryParam(UuidDto.NAME) @Parameter(description = "Behandling uuid") @Valid UuidDto queryParam) {
        var behandling = behandlingRepository.hentBehandling(queryParam.getBehandlingUuid());
        return vergeTjeneste.hentVerge(behandling);
    }

    @GET
    @Path(VERGE_BACKEND_PART_PATH)
    @Operation(description = "Henter verge/fullmektig på behandlingen for bruk backend", tags = "verge", responses = {
        @ApiResponse(responseCode = "200", description = "Verge hvis eksisterer ellers null", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VergeBackendDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public VergeBackendDto getVergeBackend(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                           @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto queryParam) {
        var behandling = behandlingRepository.hentBehandling(queryParam.getBehandlingUuid());
        return vergeTjeneste.hentVergeForBackend(behandling);
    }

    @POST
    @Path(VERGE_OPPRETT_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter verge/fullmektig på behandlingen", tags = "verge", responses = {@ApiResponse(responseCode = "200", description = "Verge/fullmektig opprettes", headers = @Header(name = HttpHeaders.LOCATION))})
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response opprettVerge(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class) @QueryParam(UuidDto.NAME) @Parameter(description = "Behandling uuid") @Valid UuidDto queryParam,
                                 @TilpassetAbacAttributt(supplierClass = NyVergeDtoAbacSupplier.class) @Valid VergeDto body) {

        var behandling = behandlingRepository.hentBehandling(queryParam.getBehandlingUuid());
        vergeTjeneste.opprettVerge(behandling, body, null);

        return Response.ok().build();
    }

    @POST
    @Path(VERGE_FJERN_PART_PATH)
    @Operation(tags = "verge", description = "Fjerner verge/fullmektig på behandlingen", responses = {@ApiResponse(responseCode = "200", description = "Verge/fullmektig fjernet", headers = @Header(name = HttpHeaders.LOCATION))})
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response fjernVerge(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class) @QueryParam(UuidDto.NAME) @Parameter(description = "Behandling uuid") @Valid UuidDto queryParam) {
        var lås = behandlingRepository.taSkriveLås(queryParam.getBehandlingUuid());
        var behandling = behandlingRepository.hentBehandling(queryParam.getBehandlingUuid());
        vergeTjeneste.fjernVerge(behandling, lås);

        return Response.ok().build();
    }

    public static class NyVergeDtoAbacSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (VergeDto) obj;
            var attributter = AbacDataAttributter.opprett();
            Optional.ofNullable(req.fnr()).ifPresent(f -> attributter.leggTil(AppAbacAttributtType.FNR, f));
            return attributter;
        }
    }
}
