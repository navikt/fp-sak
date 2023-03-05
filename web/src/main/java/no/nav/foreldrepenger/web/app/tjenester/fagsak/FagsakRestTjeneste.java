package no.nav.foreldrepenger.web.app.tjenester.fagsak;

import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.Redirect;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.ProsessTaskGruppeIdDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.app.FagsakFullTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.app.FagsakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.FagsakBackendDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.FagsakFullDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.FagsakSøkDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SokefeltDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(FagsakRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class FagsakRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(FagsakRestTjeneste.class);

    static final String BASE_PATH = "/fagsak";
    private static final String FAGSAK_PART_PATH = "";
    public static final String FAGSAK_PATH = BASE_PATH;
    private static final String FAGSAK_FULL_PART_PATH = "/full";
    public static final String FAGSAK_FULL_PATH = BASE_PATH + FAGSAK_FULL_PART_PATH;
    private static final String STATUS_PART_PATH = "/status";
    public static final String STATUS_PATH = BASE_PATH + STATUS_PART_PATH;
    private static final String SOK_PART_PATH = "/sok";
    public static final String SOK_PATH = BASE_PATH + SOK_PART_PATH;

    private FagsakTjeneste fagsakTjeneste;
    private FagsakFullTjeneste fagsakFullTjeneste;

    public FagsakRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public FagsakRestTjeneste(FagsakTjeneste fagsakTjeneste, FagsakFullTjeneste fagsakFullTjeneste) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.fagsakFullTjeneste = fagsakFullTjeneste;
    }

    @GET
    @Path(STATUS_PART_PATH)
    @Operation(description = "Url for å polle på fagsak mens behandlingprosessen pågår i bakgrunnen(asynkront)", summary = "Returnerer link til enten samme (hvis ikke ferdig) eller redirecter til /fagsak dersom asynkrone operasjoner er ferdig.", tags = "fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Status", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AsyncPollingStatus.class))),
            @ApiResponse(responseCode = "303", description = "Pågående prosesstasks avsluttet", headers = @Header(name = HttpHeaders.LOCATION)),
            @ApiResponse(responseCode = "418", description = "ProsessTasks har feilet", headers = @Header(name = HttpHeaders.LOCATION), content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AsyncPollingStatus.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Response hentFagsakMidlertidigStatus(@Context HttpServletRequest request,
                                                @TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class) @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto idDto,
                                                @TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.TaskgruppeAbacDataSupplier.class) @QueryParam("gruppe") @Valid ProsessTaskGruppeIdDto gruppeDto)
        throws URISyntaxException {
        var saksnummer = new Saksnummer(idDto.getVerdi());
        var gruppe = gruppeDto == null ? null : gruppeDto.getGruppe();
        var prosessTaskGruppePågår = fagsakTjeneste.sjekkProsessTaskPågår(saksnummer, gruppe);
        // TODO (jol): gjennomgå når det er riktig å oppdatere hele fagsakkonteksten, evt kalle lenke "sak-alle-behandlinger". Behandlingsoppretting er inkonsekvent
        return Redirect.tilFagsakEllerPollStatus(request, saksnummer, prosessTaskGruppePågår.orElse(null));
    }

    @GET
    @Path(FAGSAK_FULL_PART_PATH)
    @Operation(description = "Hent full fagsaksaksinformasjon for saksnummer", tags = "fagsak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer fagsak", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FagsakFullDto.class))),
        @ApiResponse(responseCode = "404", description = "Fagsak ikke tilgjengelig")
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Response hentFullFagsak(@Context HttpServletRequest request,
                                   @TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class) @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto s) {
        var saksnummer = new Saksnummer(s.getVerdi());
        return fagsakFullTjeneste.hentFullFagsakDtoForSaksnummer(request, saksnummer)
            .map(f -> Response.ok(f).build())
            .orElseGet(() -> Response.status(Response.Status.FORBIDDEN).build()); // Etablert praksis
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    // re-enable hvis endres til ikke-tom @Path(FAGSAK_PART_PATH)
    @Operation(description = "Hent fagsak for saksnummer", tags = "fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer fagsak", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FagsakBackendDto.class))),
            @ApiResponse(responseCode = "404", description = "Fagsak ikke tilgjengelig")
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Response hentFagsak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
        @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto s) {

        var saksnummer = new Saksnummer(s.getVerdi());
        return fagsakTjeneste.hentFagsakDtoForSaksnummer(saksnummer)
            .map(f -> Response.ok(f).build())
            .orElseGet(() -> Response.status(Response.Status.FORBIDDEN).build()); // Etablert praksis
    }

    @POST
    @Path(SOK_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Søk etter saker på saksnummer eller fødselsnummer", tags = "fagsak", summary = ("Spesifikke saker kan søkes via saksnummer. "
        +
        "Oversikt over saker knyttet til en bruker kan søkes via fødselsnummer eller d-nummer."))
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public List<FagsakSøkDto> sokFagsaker(@TilpassetAbacAttributt(supplierClass = SøkeFeltAbacDataSupplier.class)
        @Parameter(description = "Søkestreng kan være saksnummer, fødselsnummer eller D-nummer.") @Valid SokefeltDto søkestreng) {
        return fagsakTjeneste.søkFagsakDto(søkestreng.getSearchString().trim());
    }

    public static class SøkeFeltAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (SokefeltDto) obj;
            var attributter = AbacDataAttributter.opprett();
            if (req.getSearchString().length() == 13 /* guess - aktørId */) {
                attributter.leggTil(AppAbacAttributtType.AKTØR_ID, req.getSearchString())
                    .leggTil(AppAbacAttributtType.SAKER_FOR_AKTØR, req.getSearchString());
            } else {
                attributter.leggTil(AppAbacAttributtType.SAKSNUMMER, req.getSearchString());
            }
            return attributter;
        }
    }

}
