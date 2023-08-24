package no.nav.foreldrepenger.web.app.tjenester.behandling.verge;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
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

import java.net.URISyntaxException;
import java.util.Optional;

@ApplicationScoped
@Transactional
@Path(VergeRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class VergeRestTjeneste {

    static final String BASE_PATH = "/verge";
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

    @POST
    @Path(VERGE_OPPRETT_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter aksjonspunkt for verge/fullmektig på behandlingen", tags = "verge", responses = {
            @ApiResponse(responseCode = "200", description = "Aksjonspunkt for verge/fullmektig opprettes", headers = @Header(name = HttpHeaders.LOCATION))
    })
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
        return Redirect.tilBehandlingPollStatus(request, behandling.getUuid(), Optional.empty());
    }

    @POST
    @Path(VERGE_FJERN_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner aksjonspunkt og evt. registrert informasjon om verge/fullmektig fra behandlingen", tags = "verge", responses = {
            @ApiResponse(responseCode = "200", description = "Fjerning av verge/fullmektig er gjennomført", headers = @Header(name = HttpHeaders.LOCATION))
    })
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
        return Redirect.tilBehandlingPollStatus(request, behandling.getUuid(), Optional.empty());
    }

    private Behandling getBehandling(BehandlingIdDto behandlingIdDto) {
        return behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());
    }
}
