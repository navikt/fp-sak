package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

/**
 * Denne finnes utelukkende pga autotest
 */

@Path("/historikk")
@ApplicationScoped
@Transactional
public class HistorikkRestTjeneste {
    private HistorikkTjenesteAdapter historikkTjeneste;
    private HistorikkDtoTjeneste historikkDtoTjeneste;

    public static final String HISTORIKK_PATH = "/historikk";

    public HistorikkRestTjeneste() {
        // Rest CDI
    }

    @Inject
    public HistorikkRestTjeneste(HistorikkTjenesteAdapter historikkTjeneste, HistorikkDtoTjeneste historikkDtoTjeneste) {
        this.historikkTjeneste = historikkTjeneste;
        this.historikkDtoTjeneste = historikkDtoTjeneste;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter alle historikkinnslag for en gitt sak.", tags = "historikk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Response hentAlleInnslag(@Context HttpServletRequest request,
                                    @NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer må være et eksisterende saksnummer")
                                    @TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                    @Valid SaksnummerDto saksnummerDto) {
        var url = HistorikkRequestPath.getRequestPath(request);

        var historikkInnslagDtoList = historikkTjeneste.hentAlleHistorikkInnslagForSak(new Saksnummer(saksnummerDto.getVerdi()), url);
        return Response.ok().entity(historikkInnslagDtoList).build();
    }

    @GET
    @Path("/v2")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter alle historikkinnslag for en gitt sak.", tags = "historikk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Response hentAlleInnslag(@NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer må være et eksisterende saksnummer")
                                    @TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                    @Valid SaksnummerDto saksnummerDto) {
        var historikkinnslag = historikkDtoTjeneste.hentForSak(new Saksnummer(saksnummerDto.getVerdi()));
        return Response.ok().entity(historikkinnslag).build();
    }

}
