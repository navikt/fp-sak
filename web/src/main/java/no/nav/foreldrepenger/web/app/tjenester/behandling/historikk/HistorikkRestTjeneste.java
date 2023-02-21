package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

    public static final String HISTORIKK_PATH = "/historikk";

    public HistorikkRestTjeneste() {
        // Rest CDI
    }

    @Inject
    public HistorikkRestTjeneste(HistorikkTjenesteAdapter historikkTjeneste) {
        this.historikkTjeneste = historikkTjeneste;
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

}
