package no.nav.foreldrepenger.web.app;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;

/**
 * DummyRestTjeneste returnerer alltid tomt resultat. Klienten for tilbakekreving krever at det retureres en verdi,
 * derfor kan ikke DummyRestTjeneste benyttes
 */
@Path("/dummy-fptilbake")
public class DummyFptilbakeRestTjeneste {

    @GET
    @Operation(description = "Dummy-sjekk for om det finnes en tilbakekrevingsbehandling", hidden = true)
    @Path("/behandlinger/tilbakekreving/aapen")
    public Response harÅpenTilbakekrevingBehandling(@SuppressWarnings("unused") @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        return Response.ok().entity(false).build();
    }
}
