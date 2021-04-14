package no.nav.foreldrepenger.web.app.tjenester.behandling.opptjening;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
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
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.opptjening.dto.OpptjeningDto;
import no.nav.foreldrepenger.domene.opptjening.dto.OpptjeningDtoTjeneste;
import no.nav.foreldrepenger.domene.opptjening.dto.OpptjeningIUtlandDokStatusDto;
import no.nav.foreldrepenger.domene.opptjening.dto.OpptjeningIUtlandDokStatusDtoTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

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
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private OpptjeningIUtlandDokStatusDtoTjeneste opptjeningIUtlandDokStatusDtoTjeneste;

    public OpptjeningRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public OpptjeningRestTjeneste(BehandlingRepository behandlingRepository,
            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
            OpptjeningDtoTjeneste dtoMapper, OpptjeningIUtlandDokStatusDtoTjeneste opptjeningIUtlandDokStatusDtoTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.dtoMapper = dtoMapper;
        this.opptjeningIUtlandDokStatusDtoTjeneste = opptjeningIUtlandDokStatusDtoTjeneste;
    }

    @GET
    @Path(OPPTJENING_PART_PATH)
    @Operation(description = "Hent informasjon om opptjening", tags = "opptjening", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Opptjening, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OpptjeningDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public OpptjeningDto getOpptjening(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return getOpptjeningFraBehandling(behandling);
    }

    @GET
    @Path(UTLAND_DOK_STATUS_PART_PATH)
    @Operation(description = "Henters saksbehandlers valg om det skal innhentes dok fra utland", tags = "opptjening", responses = {
            @ApiResponse(responseCode = "200", description = "Om dok skal hentes eller ikke", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OpptjeningIUtlandDokStatusDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public OpptjeningIUtlandDokStatusDto getDokStatus(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandlingHvisFinnes(uuidDto.getBehandlingUuid());
        if (behandling.isEmpty()) {
            return null;
        }
        return opptjeningIUtlandDokStatusDtoTjeneste.mapFra(behandlingRef(behandling.get())).orElse(null);
    }

    private OpptjeningDto getOpptjeningFraBehandling(Behandling behandling) {
        var behandlingReferanse = behandlingRef(behandling);
        return dtoMapper.mapFra(behandlingReferanse).orElse(null);
    }

    private BehandlingReferanse behandlingRef(Behandling behandling) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        return BehandlingReferanse.fra(behandling, skjæringstidspunkt);
    }
}
