package no.nav.foreldrepenger.web.app.tjenester.formidling;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingFormidlingDtoTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

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

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;


@Path(FormidlingRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
// Tilbyr data til fp-formidling, formidlingsløsning ut mot søker.
public class FormidlingRestTjeneste {

    public static final String BASE_PATH = "/formidling";
    public static final String RESSURSER_PART_PATH = "/ressurser";
    public static final String RESSURSER_PATH = BASE_PATH + RESSURSER_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private BehandlingFormidlingDtoTjeneste behandlingFormidlingDtoTjeneste;

    @Inject
    public FormidlingRestTjeneste(BehandlingRepository behandlingRepository,
                                  BehandlingFormidlingDtoTjeneste behandlingFormidlingDtoTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingFormidlingDtoTjeneste = behandlingFormidlingDtoTjeneste;
    }

    public FormidlingRestTjeneste() {
    }

    @GET
    @Path(RESSURSER_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Hent behandling med tilhørende ressurslenker for bruk i brev", tags = "brev")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response hentBehandlingDtoForBrev(@NotNull @Parameter(description = "Id eller UUID for behandlingen") @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) {
        if (behandlingIdDto.getBehandlingUuid() != null) {
            var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingIdDto.getBehandlingUuid());
            var dto = behandling.map(value -> behandlingFormidlingDtoTjeneste.lagDtoForBrev(value)).orElse(null);
            Response.ResponseBuilder responseBuilder = Response.ok().entity(dto);
            return responseBuilder.build();
        } else {
            var behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingId());
            var dto = behandling != null ? behandlingFormidlingDtoTjeneste.lagDtoForBrev(behandling) : null;
            Response.ResponseBuilder responseBuilder = Response.ok().entity(dto);
            return responseBuilder.build();
        }
    }

}
