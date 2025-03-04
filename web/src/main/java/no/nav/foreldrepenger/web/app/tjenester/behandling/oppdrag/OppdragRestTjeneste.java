package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

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
    public static final String OPPDRAGINFO_PATH = BASE_PATH + OPPDRAGINFO_PART_PATH;

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
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    @Path(OPPDRAGINFO_PART_PATH)
    public OppdragDto hentOppdrag(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var oppdragskontroll = økonomioppdragRepository.finnOppdragForBehandling(behandling.getId());
        return oppdragskontroll
                .map(OppdragDto::new)
                .orElse(null);
    }

}
