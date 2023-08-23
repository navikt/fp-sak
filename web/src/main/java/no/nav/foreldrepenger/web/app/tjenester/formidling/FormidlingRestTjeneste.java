package no.nav.foreldrepenger.web.app.tjenester.formidling;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingFormidlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.utsattoppstart.StartdatoUtsattDto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.utsattoppstart.StartdatoUtsattDtoTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Path(FormidlingRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
// Tilbyr data til fp-formidling, formidlingsløsning ut mot søker.
public class FormidlingRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(FormidlingRestTjeneste.class);

    public static final String BASE_PATH = "/formidling";
    public static final String RESSURSER_PART_PATH = "/ressurser";
    public static final String RESSURSER_PATH = BASE_PATH + RESSURSER_PART_PATH;
    public static final String UTSATT_START_PART_PATH = "/utsattstart";
    public static final String UTSATT_START_PATH = BASE_PATH + UTSATT_START_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private BehandlingFormidlingDtoTjeneste behandlingFormidlingDtoTjeneste;
    private StartdatoUtsattDtoTjeneste startdatoUtsattDtoTjeneste;

    @Inject
    public FormidlingRestTjeneste(BehandlingRepository behandlingRepository,
                                  BehandlingFormidlingDtoTjeneste behandlingFormidlingDtoTjeneste,
                                  StartdatoUtsattDtoTjeneste startdatoUtsattDtoTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingFormidlingDtoTjeneste = behandlingFormidlingDtoTjeneste;
        this.startdatoUtsattDtoTjeneste = startdatoUtsattDtoTjeneste;
    }

    public FormidlingRestTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    @GET
    @Path(RESSURSER_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Hent behandling med tilhørende ressurslenker for bruk i formidling", tags = "formidling")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Response hentBehandlingDtoForBrev(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class)
        @NotNull @Parameter(description = "UUID for behandlingen") @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) {
        var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingIdDto.getBehandlingUuid());
        var dto = behandling.map(value -> behandlingFormidlingDtoTjeneste.lagDtoForFormidling(value)).orElse(null);
        var responseBuilder = Response.ok().entity(dto);
        return responseBuilder.build();
    }

    @GET
    @Path(UTSATT_START_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Hent informasjon om sak er utsatt fra start og evt ny dato", tags = "formidling")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Response utsattStartdato(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandlingHvisFinnes(uuidDto.getBehandlingUuid());
        var dto = behandling.map(value -> startdatoUtsattDtoTjeneste.getInformasjonOmUtsettelseFraStart(value))
            .orElse(new StartdatoUtsattDto(false, null));
        var responseBuilder = Response.ok().entity(dto);
        return responseBuilder.build();
    }

}
