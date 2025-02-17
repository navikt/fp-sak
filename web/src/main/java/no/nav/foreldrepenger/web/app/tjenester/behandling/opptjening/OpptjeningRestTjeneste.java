package no.nav.foreldrepenger.web.app.tjenester.behandling.opptjening;

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
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.opptjening.dto.OpptjeningDto;
import no.nav.foreldrepenger.domene.opptjening.dto.OpptjeningDtoTjeneste;
import no.nav.foreldrepenger.domene.opptjening.dto.OpptjeningIUtlandDokStatusDto;
import no.nav.foreldrepenger.domene.opptjening.dto.OpptjeningIUtlandDokStatusDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@ApplicationScoped
@Path(OpptjeningRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class OpptjeningRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String OPPTJENING_PART_PATH = "/opptjening";
    private static final String UTLAND_DOK_STATUS_PART_PATH = "/opptjening/utlanddokstatus";
    public static final String OPPTJENING_PATH = BASE_PATH + OPPTJENING_PART_PATH;
    public static final String UTLAND_DOK_STATUS_PATH = BASE_PATH + UTLAND_DOK_STATUS_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private OpptjeningDtoTjeneste dtoMapper;

    private OpptjeningIUtlandDokStatusDtoTjeneste opptjeningIUtlandDokStatusDtoTjeneste;

    public OpptjeningRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public OpptjeningRestTjeneste(BehandlingRepository behandlingRepository,
                                  OpptjeningDtoTjeneste dtoMapper,
                                  OpptjeningIUtlandDokStatusDtoTjeneste opptjeningIUtlandDokStatusDtoTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.dtoMapper = dtoMapper;
        this.opptjeningIUtlandDokStatusDtoTjeneste = opptjeningIUtlandDokStatusDtoTjeneste;
    }

    @GET
    @Path(OPPTJENING_PART_PATH)
    @Operation(description = "Hent informasjon om opptjening", tags = "opptjening", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Opptjening, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OpptjeningDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public OpptjeningDto getOpptjening(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return getOpptjeningFraBehandling(behandling);
    }

    @GET
    @Path(UTLAND_DOK_STATUS_PART_PATH)
    @Operation(description = "Henters saksbehandlers valg om det skal innhentes dok fra utland", tags = "opptjening", responses = {
            @ApiResponse(responseCode = "200", description = "Om dok skal hentes eller ikke", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OpptjeningIUtlandDokStatusDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public OpptjeningIUtlandDokStatusDto getDokStatus(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return behandlingRepository.hentBehandlingHvisFinnes(uuidDto.getBehandlingUuid())
            .flatMap(opptjeningIUtlandDokStatusDtoTjeneste::mapFra)
            .orElse(null);
    }

    private OpptjeningDto getOpptjeningFraBehandling(Behandling behandling) {
        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        return dtoMapper.mapFra(behandlingReferanse).orElse(null);
    }
}
