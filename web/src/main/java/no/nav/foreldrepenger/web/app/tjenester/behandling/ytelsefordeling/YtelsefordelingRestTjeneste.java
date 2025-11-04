package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

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
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(YtelsefordelingRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class YtelsefordelingRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String YTELSESFORDELING_PART_PATH = "/ytelsefordeling";
    public static final String YTELSESFORDELING_PATH = BASE_PATH + YTELSESFORDELING_PART_PATH;
    private static final String OMSORG_OG_RETT_PART_PATH = "/omsorg-og-rett";
    public static final String OMSORG_OG_RETT_PATH = BASE_PATH + OMSORG_OG_RETT_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private YtelseFordelingDtoTjeneste dtoMapper;

    public YtelsefordelingRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public YtelsefordelingRestTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider, YtelseFordelingDtoTjeneste dtoMapper) {
        this.dtoMapper = dtoMapper;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
    }

    @GET
    @Path(YTELSESFORDELING_PART_PATH)
    @Operation(description = "Hent informasjon om ytelsefordeling", tags = "ytelsefordeling", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Ytelsefordeling mellom parter, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = YtelseFordelingDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public YtelseFordelingDto getYtelsefordeling(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return dtoMapper.mapFra(BehandlingReferanse.fra(behandling)).orElse(null);
    }

    @GET
    @Path(OMSORG_OG_RETT_PART_PATH)
    @Operation(description = "Hent informasjon om omsorg og rett", responses = {@ApiResponse(responseCode = "200", description = "Returnerer informasjon fra søknad og registerdata som omhandler brukers og annen parts rett og omsorg i behandlingen", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OmsorgOgRettDto.class)))})
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public OmsorgOgRettDto hentRettOgOmsorg(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class) @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return dtoMapper.mapFra(uuidDto.getBehandlingUuid()).orElse(null);
    }

}
