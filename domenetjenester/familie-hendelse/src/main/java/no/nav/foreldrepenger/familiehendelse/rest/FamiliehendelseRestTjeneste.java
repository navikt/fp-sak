package no.nav.foreldrepenger.familiehendelse.rest;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.util.Optional;

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
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path(FamiliehendelseRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class FamiliehendelseRestTjeneste {

    private BehandlingRepository behandlingRepository;
    private FamiliehendelseDataDtoTjeneste dtoMapper;

    static final String BASE_PATH = "/behandling";
    private static final String FAMILIEHENDELSE_PART_PATH = "/familiehendelse";
    public static final String FAMILIEHENDELSE_PATH = BASE_PATH + FAMILIEHENDELSE_PART_PATH;
    private static final String FAMILIEHENDELSE_V2_PART_PATH = "/familiehendelse/v2";
    public static final String FAMILIEHENDELSE_V2_PATH = BASE_PATH + FAMILIEHENDELSE_V2_PART_PATH;

    public FamiliehendelseRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FamiliehendelseRestTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider, FamiliehendelseDataDtoTjeneste dtoMapper) {
        this.dtoMapper = dtoMapper;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
    }

    @POST
    @Path(FAMILIEHENDELSE_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent informasjon om familiehendelse til grunn for ytelse", tags = "familiehendelse", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer info om familiehendelse, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FamiliehendelseDto.class)))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Deprecated
    public FamiliehendelseDto getAvklartFamiliehendelseDto(
            @NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
                ? behandlingRepository.hentBehandling(behandlingId)
                : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        Optional<FamiliehendelseDto> dtoOpt = dtoMapper.mapFra(behandling);
        return dtoOpt.orElse(null);
    }

    @GET
    @Path(FAMILIEHENDELSE_PART_PATH)
    @Operation(description = "Hent informasjon om familiehendelse til grunn for ytelse", tags = "familiehendelse", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer info om familiehendelse, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FamiliehendelseDto.class)))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    public FamiliehendelseDto getAvklartFamiliehendelseDto(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return getAvklartFamiliehendelseDto(new BehandlingIdDto(uuidDto));
    }

    @POST
    @Path(FAMILIEHENDELSE_V2_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent informasjon om familiehendelse til grunn for ytelse", tags = "familiehendelse", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer hele FamilieHendelse grunnlaget", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FamilieHendelseGrunnlagDto.class)))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Deprecated
    public FamilieHendelseGrunnlagDto getFamiliehendelseGrunnlagDto(
            @NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
                ? behandlingRepository.hentBehandling(behandlingId)
                : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        return dtoMapper.mapGrunnlagFra(behandling);
    }

    @GET
    @Path(FAMILIEHENDELSE_V2_PART_PATH)
    @Operation(description = "Hent informasjon om familiehendelse til grunn for ytelse", tags = "familiehendelse", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer hele FamilieHendelse grunnlaget", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FamilieHendelseGrunnlagDto.class)))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    public FamilieHendelseGrunnlagDto getFamiliehendelseGrunnlagDto(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return getFamiliehendelseGrunnlagDto(new BehandlingIdDto(uuidDto));
    }
}
