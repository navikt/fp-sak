package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.util.Collections;

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
import javax.ws.rs.core.UriBuilder;

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
            @NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer må være et eksisterende saksnummer") @TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
            @Valid SaksnummerDto saksnummerDto) {
        var responseBuilder = Response.ok();
        // FIXME XSS valider requestURL eller bruk relativ URL
        var requestURL = getRequestPath(request);
        var url = requestURL + "/dokument/hent-dokument";

        var historikkInnslagDtoList = historikkTjeneste
                .hentAlleHistorikkInnslagForSak(new Saksnummer(saksnummerDto.getVerdi()));
        if (historikkInnslagDtoList != null && historikkInnslagDtoList.size() > 0) {
            responseBuilder.entity(historikkInnslagDtoList);
            for (var dto : historikkInnslagDtoList) {
                for (var linkDto : dto.getDokumentLinks()) {
                    var journalpostId = linkDto.getJournalpostId();
                    var dokumentId = linkDto.getDokumentId();
                    var uriBuilder = UriBuilder.fromPath(url);
                    uriBuilder.queryParam("journalpostId", journalpostId);
                    uriBuilder.queryParam("dokumentId", dokumentId);
                    linkDto.setUrl(uriBuilder.build());
                }
            }
        } else {
            responseBuilder.entity(Collections.emptyList());
        }
        return responseBuilder.build();
    }

    String getRequestPath(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        var stringBuilder = new StringBuilder();

        stringBuilder.append(request.getScheme())
                .append("://")
                .append(request.getLocalName())
                .append(":") // NOSONAR
                .append(request.getLocalPort());

        stringBuilder.append(request.getContextPath())
                .append(request.getServletPath());
        return stringBuilder.toString();
    }
}
