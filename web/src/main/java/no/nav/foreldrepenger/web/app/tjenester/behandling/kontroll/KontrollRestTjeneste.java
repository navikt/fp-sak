package no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll;

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
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.app.KontrollDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.dto.KontrollresultatDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Denne finnes utelukkende pga fplos
 */
@Path(KontrollRestTjeneste.BASE_PATH)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class KontrollRestTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(KontrollRestTjeneste.class);

    static final String BASE_PATH = "/behandling";
    private static final String KONTROLLRESULTAT_V2_PART_PATH = "/kontrollresultat/resultat";
    public static final String KONTROLLRESULTAT_V2_PATH = BASE_PATH + KONTROLLRESULTAT_V2_PART_PATH;

    private KontrollDtoTjeneste kontrollDtoTjeneste;
    private BehandlingRepository behandlingRepository;

    public KontrollRestTjeneste() {
        // CDI
    }

    @Inject
    public KontrollRestTjeneste(KontrollDtoTjeneste kontrollDtoTjeneste, BehandlingRepository behandlingRepository) {
        this.kontrollDtoTjeneste = kontrollDtoTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Operation(description = "Hent kontrollresultatet for en behandling", tags = "kontroll", responses = {
        @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = KontrollresultatDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    @Path(KONTROLLRESULTAT_V2_PART_PATH)
    public KontrollresultatDto hentKontrollresultatV2(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                      @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        LOG.info("Kall på deprekert endepunkt kontrollresultat");
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var referanse = BehandlingReferanse.fra(behandling);
        return kontrollDtoTjeneste.lagKontrollresultatForBehandling(referanse).orElse(null);
    }

}
