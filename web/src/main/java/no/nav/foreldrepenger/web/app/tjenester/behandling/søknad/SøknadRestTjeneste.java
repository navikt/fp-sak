package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Produces(MediaType.APPLICATION_JSON)
@Path(SøknadRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class SøknadRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String SOKNAD_PART_PATH = "/soknad";
    public static final String SOKNAD_PATH = BASE_PATH + SOKNAD_PART_PATH;
    private static final String SOKNAD_BACKEND_PART_PATH = "/soknad-backend";
    public static final String SOKNAD_BACKEND_PATH = BASE_PATH + SOKNAD_BACKEND_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private SøknadDtoTjeneste dtoMapper;

    public SøknadRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public SøknadRestTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider, SøknadDtoTjeneste dtoMapper) {
        this.dtoMapper = dtoMapper;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();

    }

    @GET
    @Path(SOKNAD_PART_PATH)
    @Operation(description = "Hent informasjon om søknad", tags = "søknad", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Søknad, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SoknadDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public SoknadDto getSøknad(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return dtoMapper.mapFra(behandling).orElse(null);
    }

    @GET
    @Path(SOKNAD_BACKEND_PART_PATH)
    @Operation(description = "Hent informasjon om søknad", tags = "søknad", responses = {
        @ApiResponse(responseCode = "200", description = "RReturnerer forenklet søknad til andre applikasjoner", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SoknadDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public SoknadBackendDto getSøknadBackend(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return dtoMapper.mapForBackend(behandling).orElse(null);
    }
}
