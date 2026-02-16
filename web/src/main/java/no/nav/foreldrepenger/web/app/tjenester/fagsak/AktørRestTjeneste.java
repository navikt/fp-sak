package no.nav.foreldrepenger.web.app.tjenester.fagsak;

import java.util.function.Function;

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
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.web.app.exceptions.FeilDto;
import no.nav.foreldrepenger.web.app.exceptions.FeilType;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.app.FagsakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.AktoerIdDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.AktørInfoDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@ApplicationScoped
@Transactional
@Path("/aktoer-info")
@Produces(MediaType.APPLICATION_JSON)
public class AktørRestTjeneste {

    private FagsakTjeneste fagsakTjeneste;

    public AktørRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AktørRestTjeneste(FagsakTjeneste fagsakTjeneste) {
        this.fagsakTjeneste = fagsakTjeneste;
    }

    @GET
    @Operation(description = "Henter informasjon om en aktør", tags = "aktoer", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer basisinformasjon om en aktør og hvilke fagsaker vedkommede har i fpsak.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AktørInfoDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response getAktørInfo(@TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class) @NotNull @QueryParam("aktoerId") @Valid AktoerIdDto aktørIdDto) {
        var aktørId = aktørIdDto.get();
        if (aktørId.isPresent()) {
            return fagsakTjeneste.lagAktørInfoDto(aktørId.get()).map(a -> Response.ok(a).build())
                .orElseGet(() -> Response.ok(new FeilDto(FeilType.TOMT_RESULTAT_FEIL, "Finner ingen aktør med denne ideen.")).status(Response.Status.NOT_FOUND).build());
        }
        var feilDto = new FeilDto(FeilType.GENERELL_FEIL, "Query parameteret 'aktoerId' mangler i forespørselen.");
        return Response.ok(feilDto).status(Response.Status.BAD_REQUEST).build();

    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (AktoerIdDto) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, req.getAktoerId());
        }
    }

}
