package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.util.List;

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
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Sak hentSak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class) @NotNull @Parameter(description = "Saksnummer for fagsak") @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = saksnummerDto.getVerdi();
        try {
            return dtoTjeneste.hentSak(saksnummer);
        } catch (Exception e) {
            LOG.warn("Oppslag av sak for fpoversikt feilet", e);
            throw e;
        }
    }

    @GET
    @Path("manglendeVedlegg")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter manglede vedlegg på sak", tags = "fpoversikt")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public List<DokumentTyperDto> hentmanglendeVedlegg(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class) @NotNull @Parameter(description = "Saksnummer for fagsak") @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = saksnummerDto.getVerdi();
        try {
            return dtoTjeneste.hentManglendeVedleggForSak(saksnummer);
        } catch (Exception e) {
            LOG.warn("Oppslag av manglende vedlegg på sak for fpoversikt feilet", e);
            throw e;
        }
    }

    @GET
    @Path("inntektsmeldinger")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter inntektsmeldinger på sak", tags = "fpoversikt")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public List<FpSakInntektsmeldingDto> hentInntektsmeldinger(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class) @NotNull @Parameter(description = "Saksnummer for fagsak") @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = saksnummerDto.getVerdi();
        return dtoTjeneste.hentInntektsmeldingerForSak(saksnummer);
    }
}
