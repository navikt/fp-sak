package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel;

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
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.FødselDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(FødselRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class FødselRestTjeneste {

    static final String BASE_PATH = "/behandling/fodsel";
    private static final String FAKTA_FODSEL_PART_PATH = "/fakta-fodsel";
    public static final String FAKTA_FODSEL_PATH = BASE_PATH + FAKTA_FODSEL_PART_PATH; // TODO: Sjekk om denne skal legges inn flere steder

    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;
    private FaktaFødselTjeneste faktaFødselTjeneste;

    @Inject
    public FødselRestTjeneste(BehandlingsprosessTjeneste behandlingsprosessTjeneste, FaktaFødselTjeneste faktaFødselTjeneste) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.faktaFødselTjeneste = faktaFødselTjeneste;
    }

    FødselRestTjeneste() {
        // for CDI proxy
    }

    @Path(FAKTA_FODSEL_PART_PATH)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent fakta om fødsel i behandling", tags = "behandling - fødsel", responses = {@ApiResponse(responseCode = "200", description = "Returnerer Fakta om fødsel og termin)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FødselDto.class)))})
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public FødselDto hentFødsel(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class) @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandlingId = behandlingsprosessTjeneste.hentBehandling(uuidDto.getBehandlingUuid()).getId();
        return faktaFødselTjeneste.hentFaktaOmFødsel(behandlingId);
    }
}
