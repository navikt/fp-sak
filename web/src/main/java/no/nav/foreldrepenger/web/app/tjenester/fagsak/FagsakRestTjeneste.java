package no.nav.foreldrepenger.web.app.tjenester.fagsak;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsoppretterTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingOpprettingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.Redirect;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.SakRettigheterDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.ProsessTaskGruppeIdDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.app.FagsakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.app.FagsakSamlingForBruker;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.FagsakBackendDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.FagsakDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.PersonDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SokefeltDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;

@Path(FagsakRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class FagsakRestTjeneste {

    static final String BASE_PATH = "/fagsak";
    private static final String FAGSAK_PART_PATH = "";
    public static final String FAGSAK_PATH = BASE_PATH;
    private static final String FAGSAK_BACKEND_PART_PATH = "/backend";
    public static final String FAGSAK_BACKEND_PATH = BASE_PATH + FAGSAK_BACKEND_PART_PATH;
    private static final String STATUS_PART_PATH = "/status";
    public static final String STATUS_PATH = BASE_PATH + STATUS_PART_PATH;
    private static final String BRUKER_PART_PATH = "/bruker";
    public static final String BRUKER_PATH = BASE_PATH + BRUKER_PART_PATH;
    private static final String RETTIGHETER_PART_PATH = "/rettigheter";
    public static final String RETTIGHETER_PATH = BASE_PATH + RETTIGHETER_PART_PATH;
    private static final String SOK_PART_PATH = "/sok";
    public static final String SOK_PATH = BASE_PATH + SOK_PART_PATH; // NOSONAR TFP-2234

    private FagsakTjeneste fagsakTjeneste;
    private BehandlingsoppretterTjeneste behandlingsoppretterTjeneste;

    public FagsakRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public FagsakRestTjeneste(FagsakTjeneste fagsakTjeneste,
                              BehandlingsoppretterTjeneste behandlingsoppretterTjeneste) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.behandlingsoppretterTjeneste = behandlingsoppretterTjeneste;
    }

    @GET
    @Path(STATUS_PART_PATH)
    @Operation(description = "Url for å polle på fagsak mens behandlingprosessen pågår i bakgrunnen(asynkront)", summary = "Returnerer link til enten samme (hvis ikke ferdig) eller redirecter til /fagsak dersom asynkrone operasjoner er ferdig.", tags = "fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Status", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AsyncPollingStatus.class))),
            @ApiResponse(responseCode = "303", description = "Pågående prosesstasks avsluttet", headers = @Header(name = HttpHeaders.LOCATION)),
            @ApiResponse(responseCode = "418", description = "ProsessTasks har feilet", headers = @Header(name = HttpHeaders.LOCATION), content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AsyncPollingStatus.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response hentFagsakMidlertidigStatus(@NotNull @QueryParam("saksnummer") @Valid SaksnummerDto idDto,
            @QueryParam("gruppe") @Valid ProsessTaskGruppeIdDto gruppeDto) {
        Saksnummer saksnummer = new Saksnummer(idDto.getVerdi());
        String gruppe = gruppeDto == null ? null : gruppeDto.getGruppe();
        Optional<AsyncPollingStatus> prosessTaskGruppePågår = fagsakTjeneste.sjekkProsessTaskPågår(saksnummer, gruppe);
        return Redirect.tilFagsakEllerPollStatus(saksnummer, prosessTaskGruppePågår.orElse(null));
    }

    @GET
    @Path(BRUKER_PART_PATH)
    @Operation(description = "Hent brukerdata for aktørId", tags = "fagsak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer person", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PersonDto.class))),
        @ApiResponse(responseCode = "404", description = "Person ikke tilgjengelig")
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response hentBrukerForFagsak(@NotNull @QueryParam("saksnummer") @Valid SaksnummerDto s) {
        Saksnummer saksnummer = new Saksnummer(s.getVerdi());
        var personInfo = fagsakTjeneste.hentBruker(saksnummer);
        if (personInfo.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var dto = mapFraPersoninfoBasis(personInfo.get());
        return Response.ok(dto).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(FAGSAK_PART_PATH)
    @Operation(description = "Hent fagsak for saksnummer", tags = "fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer fagsak", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FagsakDto.class))),
            @ApiResponse(responseCode = "404", description = "Fagsak ikke tilgjengelig")
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response hentFagsak(@NotNull @QueryParam("saksnummer") @Valid SaksnummerDto s) {

        Saksnummer saksnummer = new Saksnummer(s.getVerdi());
        FagsakSamlingForBruker view = fagsakTjeneste.hentFagsakForSaksnummer(saksnummer);
        List<FagsakDto> list = tilDtoer(view);
        if (list.isEmpty()) {
            // return 403 Forbidden istdf 404 Not Found (sikkerhet - ikke avslør for mye)
            return Response.status(Response.Status.FORBIDDEN).build();
        } else if (list.size() == 1) {
            return Response.ok(list.get(0)).build();
        } else {
            throw new IllegalStateException(
                    "Utvikler-feil: fant mer enn en fagsak for saksnummer [" + saksnummer + "], skal ikke være mulig: fant " + list.size());
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(FAGSAK_BACKEND_PART_PATH)
    @Operation(description = "Hent fagsak for saksnummer", tags = "fagsak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer fagsak", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FagsakBackendDto.class))),
        @ApiResponse(responseCode = "404", description = "Fagsak ikke tilgjengelig")
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response hentFagsakBackend(@NotNull @QueryParam("saksnummer") @Valid SaksnummerDto s) {

        Saksnummer saksnummer = new Saksnummer(s.getVerdi());
        var fagsak = fagsakTjeneste.hentFagsakForSaksnummerBackend(saksnummer);
        if (fagsak.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var dekning = fagsakTjeneste.hentDekningsgradForSaksnummerBackend(saksnummer).map(Dekningsgrad::getVerdi).orElse(null);
        var dto = new FagsakBackendDto(fagsak.get(), dekning);
        return Response.ok(dto).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(RETTIGHETER_PART_PATH)
    @Operation(description = "Hent rettigheter for saksnummer", tags = "fagsak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer rettigheter", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SakRettigheterDto.class))),
        @ApiResponse(responseCode = "404", description = "Fagsak ikke tilgjengelig")
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response hentRettigheter(@NotNull @QueryParam("saksnummer") @Valid SaksnummerDto s) {
        Saksnummer saksnummer = new Saksnummer(s.getVerdi());
        var fagsak = fagsakTjeneste.hentFagsakForSaksnummerBackend(saksnummer);
        if (fagsak.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var fagsakId = fagsak.map(Fagsak::getId).orElseThrow();
        var oppretting = Stream.of(BehandlingType.getYtelseBehandlingTyper(), BehandlingType.getAndreBehandlingTyper()).flatMap(Collection::stream)
            .map(bt -> new BehandlingOpprettingDto(bt, behandlingsoppretterTjeneste.kanOppretteNyBehandlingAvType(fagsakId, bt)))
            .collect(Collectors.toList());

        var dto = new SakRettigheterDto(fagsak.map(Fagsak::getSkalTilInfotrygd).orElse(false), oppretting, List.of());
        return Response.ok(dto).build();
    }

    @POST
    @Path(SOK_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Søk etter saker på saksnummer eller fødselsnummer", tags = "fagsak", summary = ("Spesifikke saker kan søkes via saksnummer. "
            +
            "Oversikt over saker knyttet til en bruker kan søkes via fødselsnummer eller d-nummer."))
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public List<FagsakDto> søkFagsaker(
            @Parameter(description = "Søkestreng kan være saksnummer, fødselsnummer eller D-nummer.") @Valid SokefeltDto søkestreng) {
        FagsakSamlingForBruker view = fagsakTjeneste.hentSaker(søkestreng.getSearchString());
        return tilDtoer(view);
    }

    private List<FagsakDto> tilDtoer(FagsakSamlingForBruker view) {
        if (view.isEmpty()) {
            return new ArrayList<>();
        }
        List<FagsakDto> dtoer = new ArrayList<>();
        List<FagsakSamlingForBruker.FagsakRad> fagsakInfoer = view.getFagsakInfoer();
        for (FagsakSamlingForBruker.FagsakRad info : fagsakInfoer) {
            Fagsak fagsak = info.getFagsak();
            Boolean kanRevurderingOpprettes = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class,
                    fagsak.getYtelseType()).orElseThrow().kanRevurderingOpprettes(fagsak);
            LocalDate fødselsdato = info.getFødselsdato();

            Integer antallBarn = info.getAntallBarn();
            var dekningsgrad = info.getDekningsgrad().map(d -> d.getVerdi()).orElse(null);
            dtoer.add(new FagsakDto(fagsak, fødselsdato, antallBarn, kanRevurderingOpprettes, fagsak.getSkalTilInfotrygd(),
                    fagsak.getRelasjonsRolleType(), dekningsgrad, FagsakTjeneste.lagLenker(fagsak), FagsakTjeneste.lagLenkerEngangshent(fagsak)));
        }
        return dtoer;
    }

    private PersonDto mapFraPersoninfoBasis(PersoninfoBasis pi) {
        return new PersonDto(pi.getNavn(), pi.getAlder(), String.valueOf(pi.getPersonIdent().getIdent()),
            pi.erKvinne(), pi.getPersonstatus(), pi.getDiskresjonskode(), pi.getDødsdato());
    }

}
