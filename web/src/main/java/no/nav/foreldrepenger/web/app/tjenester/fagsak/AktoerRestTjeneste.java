package no.nav.foreldrepenger.web.app.tjenester.fagsak;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.function.Function;

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
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.web.app.exceptions.FeilDto;
import no.nav.foreldrepenger.web.app.exceptions.FeilType;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.app.FagsakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.AktoerIdDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.AktoerInfoDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@ApplicationScoped
@Transactional
@Path(AktoerRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class AktoerRestTjeneste {

    static final String BASE_PATH = "/aktoer-info";
    private static final String AKTOER_INFO_PART_PATH = "";
    public static final String AKTOER_INFO_PATH = BASE_PATH + AKTOER_INFO_PART_PATH; // NOSONAR TFP-2234

    private FagsakTjeneste fagsakTjeneste;


    public AktoerRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AktoerRestTjeneste(FagsakTjeneste fagsakTjeneste) {
        this.fagsakTjeneste = fagsakTjeneste;
    }

    @GET
    @Operation(description = "Henter informasjon om en aktør", tags = "aktoer", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer basisinformasjon om en aktør og hvilke fagsaker vedkommede har i fpsak.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AktoerInfoDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    // re-enable hvis endres til ikke-tom @Path(AKTOER_INFO_PART_PATH)
    public Response getAktoerInfo(@TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class) @NotNull @QueryParam("aktoerId") @Valid AktoerIdDto aktoerIdDto) {
        var aktoerId = aktoerIdDto.get();
        if (aktoerId.isPresent()) {
            return fagsakTjeneste.lagAktoerInfoDto(aktoerId.get()).map(a -> Response.ok(a).build())
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
