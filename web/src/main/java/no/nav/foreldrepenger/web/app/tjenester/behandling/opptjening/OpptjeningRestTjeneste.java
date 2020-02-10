package no.nav.foreldrepenger.web.app.tjenester.behandling.opptjening;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.opptjening.dto.OpptjeningDto;
import no.nav.foreldrepenger.domene.opptjening.dto.OpptjeningDtoTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@ApplicationScoped
@Path(OpptjeningRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Transaction
public class OpptjeningRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String OPPTJENING_PART_PATH = "/opptjening";
    public static final String OPPTJENING_PATH = BASE_PATH + OPPTJENING_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private OpptjeningDtoTjeneste dtoMapper;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public OpptjeningRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public OpptjeningRestTjeneste(BehandlingRepository behandlingRepository,
                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                  OpptjeningDtoTjeneste dtoMapper) {
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.dtoMapper = dtoMapper;
    }

    @POST
    @Path(OPPTJENING_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent informasjon om opptjening",
        tags = "opptjening",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returnerer Opptjening, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = OpptjeningDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public OpptjeningDto getOpptjening(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        return getOpptjeningFraBehandling(behandling);
    }

    @GET
    @Path(OPPTJENING_PART_PATH)
    @Operation(description = "Hent informasjon om opptjening",
        tags = "opptjening",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returnerer Opptjening, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = OpptjeningDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public OpptjeningDto getOpptjening(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return getOpptjeningFraBehandling(behandling);
    }

    private OpptjeningDto getOpptjeningFraBehandling(Behandling behandling) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        BehandlingReferanse behandlingReferanse = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        return dtoMapper.mapFra(behandlingReferanse).orElse(null);
    }
}
