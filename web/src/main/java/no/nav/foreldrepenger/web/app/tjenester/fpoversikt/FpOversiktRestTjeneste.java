package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("fpoversikt")
@ApplicationScoped
@Transactional
public class FpOversiktRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(FpOversiktRestTjeneste.class);

    private FpOversiktDtoTjeneste dtoTjeneste;

    @Inject
    public FpOversiktRestTjeneste(FpOversiktDtoTjeneste dtoTjeneste) {
        this.dtoTjeneste = dtoTjeneste;
    }

    FpOversiktRestTjeneste() {
        //CDI
    }

    @GET
    @Path("sak")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent sak for bruk i fpoversikt", tags = "fpoversikt")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Sak hentSak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class) @NotNull @Parameter(description = "Saksnummer for fagsak") @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = saksnummerDto.getVerdi();
        try {
            return dtoTjeneste.hentSak(saksnummer);
        } catch (Exception e) {
            LOG.warn("Oppslag av sak for fpoversikt feilet", e);
            throw e;
        }
    }
}
