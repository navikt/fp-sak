package no.nav.foreldrepenger.web.app.tjenester.tilbake;

import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;


// Tilbyr data til fptilbake, tilbakekrevingsløsningen .
@Path(TilbakeRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class TilbakeRestTjeneste {

    public static final String BASE_PATH = "/tilbake";
    public static final String BEHANDLING_PART_PATH = "/behandling";
    public static final String HENVISNING_PART_PATH = "/henvisning";

    private BehandlingRepository behandlingRepository;
    private TilbakeBehandlingFullTjeneste behandlingFullTjeneste;

    @Inject
    public TilbakeRestTjeneste(BehandlingRepository behandlingRepository, TilbakeBehandlingFullTjeneste behandlingFullTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingFullTjeneste = behandlingFullTjeneste;
    }

    public TilbakeRestTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    @GET
    @Path(BEHANDLING_PART_PATH)
    @Operation(description = "Hent behandling for bruk i fptilbake", tags = "tilbake")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response hentBehandlingForTilbake(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                     @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandlingHvisFinnes(uuidDto.getBehandlingUuid());
        var dto = behandling.map(behandlingFullTjeneste::lagFpsakBehandlingFullDtoTjeneste).orElse(null);
        return Response.ok().entity(dto).build();
    }

    @POST
    @Path(HENVISNING_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent behandling med angitt henvisning for bruk i fptilbake", tags = "tilbake")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response hentBehandlingGittHenvisning(@TilpassetAbacAttributt(supplierClass = TilbakeRestTjeneste.HenvisningAbacDataSupplier.class)
                                     @Parameter(description = "KlageVurderingAdapter tilpasset til mellomlagring.") @Valid HenvisningRequestDto henvisningRequestDto) {
        var behandling = behandlingRepository.finnUnikBehandlingForBehandlingId(henvisningRequestDto.henvisning());
        var dto = behandling.map(behandlingFullTjeneste::lagFpsakBehandlingFullDtoTjeneste).orElse(null);
        return Response.ok().entity(dto).build();
    }

    public static class HenvisningAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (HenvisningRequestDto) obj;
            return AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.SAKSNUMMER, req.saksnummer());
        }
    }

}
