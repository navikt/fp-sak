package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoForBackendTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@ApplicationScoped
@Transactional
@Path(BehandlingBackendRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class BehandlingBackendRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String BACKEND_ROOT_PATH = "/backend-root";
    public static final String BEHANDLINGER_BACKEND_ROOT_PATH = BASE_PATH + BACKEND_ROOT_PATH;

    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste;
    private BehandlingDtoForBackendTjeneste behandlingDtoForBackendTjeneste;

    public BehandlingBackendRestTjeneste() {
        // for resteasy
    }

    @Inject
    public BehandlingBackendRestTjeneste(BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste,
                                         BehandlingDtoForBackendTjeneste behandlingDtoForBackendTjeneste) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.behandlingDtoForBackendTjeneste = behandlingDtoForBackendTjeneste;
    }

    @GET
    @Path(BACKEND_ROOT_PATH)
    @Operation(description = "Hent behandling gitt id for backend",
        summary = ("Returnerer behandlingen som er tilknyttet id. Dette er resultat etter at asynkrone operasjoner er utført."),
        tags = "behandlinger",
        responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer behandling",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = BehandlingDto.class))
                }),
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentBehandlingResultatForBackend(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingsprosessTjeneste.hentBehandling(uuidDto.getBehandlingUuid());
        AsyncPollingStatus taskStatus = behandlingsprosessTjeneste.sjekkProsessTaskPågårForBehandling(behandling, null).orElse(null);
        BehandlingDto dto = behandlingDtoForBackendTjeneste.lagBehandlingDto(behandling, taskStatus);
        ResponseBuilder responseBuilder = Response.ok().entity(dto);
        return responseBuilder.build();
    }

}
