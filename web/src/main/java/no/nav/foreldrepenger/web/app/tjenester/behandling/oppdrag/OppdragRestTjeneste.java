package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(OppdragRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class OppdragRestTjeneste {

    static final String BASE_PATH = "/behandling/oppdrag";
    private static final String OPPDRAGINFO_PART_PATH = "/oppdraginfo";
    public static final String OPPDRAGINFO_PATH = BASE_PATH + OPPDRAGINFO_PART_PATH; // NOSONAR TFP-2234

    private ØkonomioppdragRepository økonomioppdragRepository;
    private BehandlingRepository behandlingRepository;

    public OppdragRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public OppdragRestTjeneste(ØkonomioppdragRepository økonomioppdragRepository, BehandlingRepository behandlingRepository) {
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Operation(description = "Hent oppdrags-info for behandlingen", tags = "oppdrag")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    @Path(OPPDRAGINFO_PART_PATH)
    public OppdragDto hentOppdrag(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var oppdragskontroll = økonomioppdragRepository.finnOppdragForBehandling(behandling.getId());
        return oppdragskontroll
                .map(OppdragDto::fraDomene)
                .orElse(null);
    }

}
