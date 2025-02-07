package no.nav.foreldrepenger.web.app.tjenester.behandling.verge;

import java.net.URISyntaxException;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeDto;
import no.nav.foreldrepenger.web.app.rest.PathParamMap;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdVersjonDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.Redirect;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@ApplicationScoped
@Transactional
@Path(VergeRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class VergeRestTjeneste {

    public static final String BASE_PATH = "/verge";

    public static final String BEHANDLING_ID_NAME = "behandlingUuid";
    public static final String VERSJON_ID_NAME = "versjon";
    private static final String VERGE_BEHANDLING_PART_PATH = "/{" + BEHANDLING_ID_NAME + "}";
    public static final String VERGE_BEHANDLING_PATH = BASE_PATH + VERGE_BEHANDLING_PART_PATH;

    public static PathParamMap getPathParams(UUID behandlingUuid, Long behandlingVersjon) {
        return new PathParamMap()
                .add(BEHANDLING_ID_NAME, behandlingUuid)
                .add(VERSJON_ID_NAME, behandlingVersjon);
    }

    private static final String VERGE_BEHANDLING_VERSJON_PART_PATH = "/{" + BEHANDLING_ID_NAME + "}/{" + VERSJON_ID_NAME + "}";
    public static final String VERGE_BEHANDLING_VERSJON_PATH = BASE_PATH + VERGE_BEHANDLING_VERSJON_PART_PATH;

    private static final String VERGE_OPPRETT_PART_PATH = "/opprett";
    public static final String VERGE_OPPRETT_PATH = BASE_PATH + VERGE_OPPRETT_PART_PATH;
    private static final String VERGE_FJERN_PART_PATH = "/fjern";
    public static final String VERGE_FJERN_PATH = BASE_PATH + VERGE_FJERN_PART_PATH;

    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;
    private BehandlingsutredningTjeneste behandlingsutredningTjeneste;
    private VergeTjeneste vergeTjeneste;

    public VergeRestTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    @Inject
    public VergeRestTjeneste(BehandlingsprosessTjeneste behandlingsprosessTjeneste,
                             BehandlingsutredningTjeneste behandlingsutredningTjeneste,
                             VergeTjeneste vergeTjeneste) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.behandlingsutredningTjeneste = behandlingsutredningTjeneste;
        this.vergeTjeneste = vergeTjeneste;
    }

    @GET
    @Path(VERGE_BEHANDLING_PART_PATH)
    @Operation(description = "Henter verge/fullmektig på behandlingen", tags = "verge", responses = {
            @ApiResponse(responseCode = "200", description = "Verge/fullmektig funnet"),
            @ApiResponse(responseCode = "204", description = "Ingen verge/fullmektig")
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public VergeDto hentVerge(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class) @NotNull @PathParam(BEHANDLING_ID_NAME) @Valid BehandlingIdDto behandlingUuid) {

        var behandling = getBehandling(behandlingUuid);

        return vergeTjeneste.hentVerge(behandling);
    }

    @POST
    @Path(VERGE_BEHANDLING_VERSJON_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            description = "Oppretter verge/fullmektig på behandlingen",
            tags = "verge",
            responses = {@ApiResponse(responseCode = "202", description = "Verge/fullmektig opprettes", headers = @Header(name = HttpHeaders.LOCATION))})
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK)
    public Response opprettVerge(@Context HttpServletRequest request,
                                 @TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class) @PathParam(BEHANDLING_ID_NAME) BehandlingIdDto behandlingUuid,
                                 @PathParam(VERSJON_ID_NAME) @NotNull @Valid Long behandlingVersjon,
                                 @Parameter(description = "Informasjon om ny verge") @Valid NyVergeDto dto) throws URISyntaxException {

        var behandling = getBehandling(behandlingUuid);
        behandlingsutredningTjeneste.kanEndreBehandling(behandling, behandlingVersjon);
        vergeTjeneste.opprettVerge(behandling, dto);

        return Redirect.tilBehandlingPollStatus(request, behandlingUuid.getBehandlingUuid());
    }

    @DELETE
    @Path(VERGE_BEHANDLING_VERSJON_PART_PATH)
    @Operation(
            tags = "verge",
            description = "Fjerner verge/fullmektig på behandlingen",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Verge/fullmektig fjernet", headers = @Header(name = HttpHeaders.LOCATION))
            })
    @BeskyttetRessurs(actionType = ActionType.DELETE, resourceType = ResourceType.FAGSAK)
    public Response fjernVerge(@Context HttpServletRequest request,
                            @TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class)
                            @PathParam(BEHANDLING_ID_NAME) BehandlingIdDto behandlingUuid,
                            @PathParam(VERSJON_ID_NAME) @NotNull @Valid Long behandlingVersjon) throws URISyntaxException {

        var behandling = getBehandling(behandlingUuid);
        behandlingsutredningTjeneste.kanEndreBehandling(behandling, behandlingVersjon);
        vergeTjeneste.fjernVerge(behandling);

        return Redirect.tilBehandlingPollStatus(request, behandlingUuid.getBehandlingUuid());
    }


    @Deprecated(forRemoval = true)
    @POST
    @Path(VERGE_OPPRETT_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces
    @Operation(description = "Oppretter aksjonspunkt for verge/fullmektig på behandlingen", tags = "verge", responses = {@ApiResponse(responseCode = "200", description = "Aksjonspunkt for verge/fullmektig opprettes", headers = @Header(name = HttpHeaders.LOCATION))})
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK)
    public Response opprettVerge(@Context HttpServletRequest request,
                                 @TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class)
                                 @Parameter(description = "Behandling som skal få verge/fullmektig") @Valid BehandlingIdVersjonDto dto) throws URISyntaxException {
        var behandling = getBehandling(dto);
        var behandlingVersjon = dto.getBehandlingVersjon();

        // Precondition - sjekk behandling versjon/lås
        behandlingsutredningTjeneste.kanEndreBehandling(behandling, behandlingVersjon);

        vergeTjeneste.opprettVergeAksjonspunktOgHoppTilbakeTilFORVEDSTEGHvisSenereSteg(behandling);

        behandling = behandlingsprosessTjeneste.hentBehandling(behandling.getUuid());
        return Redirect.tilBehandlingPollStatus(request, behandling.getUuid());
    }

    @Deprecated(forRemoval = true)
    @POST
    @Path(VERGE_FJERN_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner aksjonspunkt og evt. registrert informasjon om verge/fullmektig fra behandlingen", tags = "verge", responses = {@ApiResponse(responseCode = "200", description = "Fjerning av verge/fullmektig er gjennomført", headers = @Header(name = HttpHeaders.LOCATION))})
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK)
    public Response fjernVerge(@Context HttpServletRequest request,
                               @TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class)
                               @Parameter(description = "Behandling som skal få fjernet verge/fullmektig") @Valid BehandlingIdVersjonDto dto) throws URISyntaxException {
        var behandling = getBehandling(dto);
        var behandlingVersjon = dto.getBehandlingVersjon();

        // Precondition - sjekk behandling versjon/lås
        behandlingsutredningTjeneste.kanEndreBehandling(behandling, behandlingVersjon);

        vergeTjeneste.fjernVergeGrunnlagOgAksjonspunkt(behandling);

        behandling = behandlingsprosessTjeneste.hentBehandling(behandling.getUuid());
        return Redirect.tilBehandlingPollStatus(request, behandling.getUuid());
    }

    private Behandling getBehandling(BehandlingIdDto behandlingIdDto) {
        return behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());
    }
}
