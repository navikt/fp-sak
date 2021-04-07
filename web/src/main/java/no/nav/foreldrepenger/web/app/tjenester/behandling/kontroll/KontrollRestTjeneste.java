package no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.app.KontrollDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.dto.KontrollresultatDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path(KontrollRestTjeneste.BASE_PATH)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class KontrollRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String KONTROLLRESULTAT_PART_PATH = "/kontrollresultat";
    public static final String KONTROLLRESULTAT_PATH = BASE_PATH + KONTROLLRESULTAT_PART_PATH; // NOSONAR TFP-2234
    private static final String KONTROLLRESULTAT_V2_PART_PATH = "/kontrollresultat/resultat";
    public static final String KONTROLLRESULTAT_V2_PATH = BASE_PATH + KONTROLLRESULTAT_V2_PART_PATH;

    private KontrollDtoTjeneste kontrollDtoTjeneste;
    private BehandlingRepository behandlingRepository;

    public KontrollRestTjeneste() {
        // resteasy
    }

    @Inject
    public KontrollRestTjeneste(KontrollDtoTjeneste kontrollDtoTjeneste, BehandlingRepository behandlingRepository) {
        this.kontrollDtoTjeneste = kontrollDtoTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Path(KONTROLLRESULTAT_PART_PATH)
    @Operation(description = "Hent kontrollresultatet for en behandling", tags = "kontroll", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = KontrollresultatDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public KontrollresultatDto hentKontrollresultat(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return kontrollDtoTjeneste.lagKontrollresultatForBehandling(behandling).orElse(null);
    }

    @GET
    @Operation(description = "Hent kontrollresultatet for en behandling", tags = "kontroll", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = KontrollresultatDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Path(KONTROLLRESULTAT_V2_PART_PATH)
    public KontrollresultatDto hentKontrollresultatV2(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return kontrollDtoTjeneste.lagKontrollresultatForBehandling(behandling).orElse(null);
    }

}
