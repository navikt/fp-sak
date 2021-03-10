package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

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

    @POST
    @Path(SOKNAD_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent informasjon om søknad", tags = "søknad", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Søknad, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SoknadDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Deprecated
    public SoknadDto getSøknad(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
                ? behandlingRepository.hentBehandling(behandlingId)
                : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        return dtoMapper.mapFra(behandling).orElse(null);
    }

    @GET
    @Path(SOKNAD_PART_PATH)
    @Operation(description = "Hent informasjon om søknad", tags = "søknad", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Søknad, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SoknadDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public SoknadDto getSøknad(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return getSøknad(new BehandlingIdDto(uuidDto));
    }

    @GET
    @Path(SOKNAD_BACKEND_PART_PATH)
    @Operation(description = "Hent informasjon om søknad", tags = "søknad", responses = {
        @ApiResponse(responseCode = "200", description = "RReturnerer forenklet søknad til andre applikasjoner", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SoknadDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public SoknadBackendDto getSøknadBackend(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return dtoMapper.mapForBackend(behandling).orElse(null);
    }
}
