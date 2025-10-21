package no.nav.foreldrepenger.web.app.tjenester.formidling;

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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;


@Path(FormidlingRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
// Tilbyr data til fp-formidling, formidlingsløsning ut mot søker.
public class FormidlingRestTjeneste {

    public static final String BASE_PATH = "/formidling";

    private BehandlingRepository behandlingRepository;
    private BrevGrunnlagTjeneste brevGrunnlagTjeneste;

    @Inject
    public FormidlingRestTjeneste(BehandlingRepository behandlingRepository,
                                  BrevGrunnlagTjeneste brevGrunnlagTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.brevGrunnlagTjeneste = brevGrunnlagTjeneste;
    }

    public FormidlingRestTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    @GET
    @Path("/grunnlag")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter felles brevdata for brevproduksjon.", tags = "formidling")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response hentBrevGrunnlag(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class)
                                         @NotNull @Parameter(description = "UUID for behandlingen") @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) {
        var dto = behandlingRepository.hentBehandlingHvisFinnes(behandlingIdDto.getBehandlingUuid())
            .map(behandling -> brevGrunnlagTjeneste.lagGrunnlagDto(behandling))
            .orElse(null);
        return Response.ok().entity(dto).build();
    }
}
