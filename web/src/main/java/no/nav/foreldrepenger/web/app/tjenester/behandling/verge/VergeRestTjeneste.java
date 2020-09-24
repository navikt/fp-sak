package no.nav.foreldrepenger.web.app.tjenester.behandling.verge;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.BehandlingIdVersjonDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeBehandlingsmenyDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.Redirect;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@ApplicationScoped
@Transactional
@Path(VergeRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class VergeRestTjeneste {

    static final String BASE_PATH = "/verge";
    private static final String VERGE_BEHANDLINGSMENY_PART_PATH = "/behandlingsmeny";
    public static final String VERGE_BEHANDLINGSMENY_PATH = BASE_PATH + VERGE_BEHANDLINGSMENY_PART_PATH;
    private static final String VERGE_OPPRETT_PART_PATH = "/opprett";
    public static final String VERGE_OPPRETT_PATH = BASE_PATH + VERGE_OPPRETT_PART_PATH;
    private static final String VERGE_FJERN_PART_PATH = "/fjern";
    public static final String VERGE_FJERN_PATH = BASE_PATH + VERGE_FJERN_PART_PATH;

    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste;
    private BehandlingsutredningApplikasjonTjeneste behandlingsutredningApplikasjonTjeneste;
    private VergeTjeneste vergeTjeneste;

    public VergeRestTjeneste() {
    }

    @Inject
    public VergeRestTjeneste(BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste,
            BehandlingsutredningApplikasjonTjeneste behandlingsutredningApplikasjonTjeneste,
            VergeTjeneste vergeTjeneste) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.behandlingsutredningApplikasjonTjeneste = behandlingsutredningApplikasjonTjeneste;
        this.vergeTjeneste = vergeTjeneste;
    }

    @GET
    @Path(VERGE_BEHANDLINGSMENY_PART_PATH)
    @Operation(description = "Instruerer hvilket menyvalg som skal være mulig fra behandlingsmenyen", tags = "verge", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer SKJUL/OPPRETT/FJERN", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VergeBehandlingsmenyDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response hentBehandlingsmenyvalg(@NotNull @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) {
        Behandling behandling = getBehandling(behandlingIdDto);
        VergeBehandlingsmenyDto dto = vergeTjeneste.utledBehandlingsmeny(behandling.getId());
        Response.ResponseBuilder responseBuilder = Response.ok().entity(dto);
        return responseBuilder.build();
    }

    @POST
    @Path(VERGE_OPPRETT_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter aksjonspunkt for verge/fullmektig på behandlingen", tags = "verge", responses = {
            @ApiResponse(responseCode = "200", description = "Aksjonspunkt for verge/fullmektig opprettes", headers = @Header(name = HttpHeaders.LOCATION))
    })
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response opprettVerge(@Parameter(description = "Behandling som skal få verge/fullmektig") @Valid BehandlingIdVersjonDto dto) {
        Behandling behandling = getBehandling(dto);
        Long behandlingVersjon = dto.getBehandlingVersjon();

        // Precondition - sjekk behandling versjon/lås
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(behandling.getId(), behandlingVersjon);

        vergeTjeneste.opprettVergeAksjonspunktOgHoppTilbakeTilKofakHvisSenereSteg(behandling);

        behandling = behandlingsprosessTjeneste.hentBehandling(behandling.getId());
        return Redirect.tilBehandlingPollStatus(behandling.getUuid(), Optional.empty());
    }

    @POST
    @Path(VERGE_FJERN_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner aksjonspunkt og evt. registrert informasjon om verge/fullmektig fra behandlingen", tags = "verge", responses = {
            @ApiResponse(responseCode = "200", description = "Fjerning av verge/fullmektig er gjennomført", headers = @Header(name = HttpHeaders.LOCATION))
    })
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response fjernVerge(@Parameter(description = "Behandling som skal få fjernet verge/fullmektig") @Valid BehandlingIdVersjonDto dto) {
        Behandling behandling = getBehandling(dto);
        Long behandlingVersjon = dto.getBehandlingVersjon();

        // Precondition - sjekk behandling versjon/lås
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(behandling.getId(), behandlingVersjon);

        vergeTjeneste.fjernVergeGrunnlagOgAksjonspunkt(behandling);

        behandling = behandlingsprosessTjeneste.hentBehandling(behandling.getId());
        return Redirect.tilBehandlingPollStatus(behandling.getUuid(), Optional.empty());
    }

    private Behandling getBehandling(@QueryParam("behandlingId") @NotNull @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        return behandlingId != null
                ? behandlingsprosessTjeneste.hentBehandling(behandlingId)
                : behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());
    }
}
