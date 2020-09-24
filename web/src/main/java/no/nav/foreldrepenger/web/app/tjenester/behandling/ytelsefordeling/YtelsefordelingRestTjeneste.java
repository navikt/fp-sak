package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

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

@Path(YtelsefordelingRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class YtelsefordelingRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String YTELSESFORDELING_PART_PATH = "/ytelsefordeling";
    public static final String YTELSESFORDELING_PATH = BASE_PATH + YTELSESFORDELING_PART_PATH;

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

    @POST
    @Path(YTELSESFORDELING_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent informasjon om ytelsefordeling", tags = "ytelsefordeling", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Ytelsefordeling mellom parter, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = YtelseFordelingDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Deprecated
    public YtelseFordelingDto getYtelsefordeling(
            @NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
                ? behandlingRepository.hentBehandling(behandlingId)
                : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        return dtoMapper.mapFra(behandling).orElse(null);
    }

    @GET
    @Path(YTELSESFORDELING_PART_PATH)
    @Operation(description = "Hent informasjon om ytelsefordeling", tags = "ytelsefordeling", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Ytelsefordeling mellom parter, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = YtelseFordelingDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public YtelseFordelingDto getYtelsefordeling(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return getYtelsefordeling(new BehandlingIdDto(uuidDto));
    }
}
