package no.nav.foreldrepenger.web.app.tjenester.behandling;

import java.util.Optional;

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
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoForBackendTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@ApplicationScoped
@Transactional
@Path(BehandlingBackendRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class BehandlingBackendRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String BACKEND_ROOT_PATH = "/backend-root";
    public static final String BEHANDLINGER_BACKEND_ROOT_PATH = BASE_PATH + BACKEND_ROOT_PATH;

    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingDtoForBackendTjeneste behandlingDtoForBackendTjeneste;

    public BehandlingBackendRestTjeneste() {
        // CDI
    }

    @Inject
    public BehandlingBackendRestTjeneste(BehandlingsprosessTjeneste behandlingsprosessTjeneste,
                                         BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                         BehandlingDtoForBackendTjeneste behandlingDtoForBackendTjeneste) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.behandlingDtoForBackendTjeneste = behandlingDtoForBackendTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    @GET
    @Path(BACKEND_ROOT_PATH)
    @Operation(description = "Hent behandling gitt id for backend", summary = "Returnerer behandlingen som er tilknyttet id. Dette er resultat etter at asynkrone operasjoner er utført.", tags = "behandlinger", responses = {@ApiResponse(responseCode = "200", description = "Returnerer behandling", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = BehandlingDto.class))}),})
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response hentBehandlingResultatForBackend(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingsprosessTjeneste.hentBehandling(uuidDto.getBehandlingUuid());
        var taskStatus = behandlingsprosessTjeneste.sjekkProsessTaskPågårForBehandling(behandling, null).orElse(null);
        var endretEnhet = sjekkEnhet(behandling);
        BehandlingDto dto = behandlingDtoForBackendTjeneste.lagBehandlingDto(behandling, taskStatus, endretEnhet);
        var responseBuilder = Response.ok().entity(dto);
        return responseBuilder.build();
    }

    private Optional<OrganisasjonsEnhet> sjekkEnhet(Behandling behandling) {
        var enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(behandling.getFagsak());
        return enhet.enhetId().equals(behandling.getBehandlendeEnhet()) ? Optional.empty() : Optional.of(enhet);
    }

}
