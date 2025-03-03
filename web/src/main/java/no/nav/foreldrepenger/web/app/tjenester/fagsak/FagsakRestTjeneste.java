package no.nav.foreldrepenger.web.app.tjenester.fagsak;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.task.OppdaterBehandlendeEnhetTask;
import no.nav.foreldrepenger.produksjonsstyring.behandlinghendelse.HendelseForBehandling;
import no.nav.foreldrepenger.produksjonsstyring.behandlinghendelse.PubliserBehandlingHendelseTask;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.Redirect;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.ProsessTaskGruppeIdDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.app.FagsakFullTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.app.FagsakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.EndreUtlandMarkeringDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.FagsakBackendDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.FagsakFullDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.FagsakSøkDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.LagreFagsakNotatDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SokefeltDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(FagsakRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class FagsakRestTjeneste {

    static final String BASE_PATH = "/fagsak";
    private static final String FAGSAK_PART_PATH = "";
    public static final String FAGSAK_PATH = BASE_PATH;
    private static final String FAGSAK_FULL_PART_PATH = "/full";
    public static final String FAGSAK_FULL_PATH = BASE_PATH + FAGSAK_FULL_PART_PATH;
    private static final String STATUS_PART_PATH = "/status";
    public static final String STATUS_PATH = BASE_PATH + STATUS_PART_PATH;
    private static final String SOK_PART_PATH = "/sok";
    public static final String SOK_PATH = BASE_PATH + SOK_PART_PATH;
    private static final String ENDRE_UTLAND_PART_PATH = "/endre-utland";
    public static final String ENDRE_UTLAND_PATH = BASE_PATH + ENDRE_UTLAND_PART_PATH;
    private static final String NOTAT_PART_PATH = "/notat";
    public static final String NOTAT_PATH = BASE_PATH + NOTAT_PART_PATH;

    private FagsakTjeneste fagsakTjeneste;
    private FagsakFullTjeneste fagsakFullTjeneste;
    private FagsakEgenskapRepository fagsakEgenskapRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private ProsessTaskTjeneste taskTjeneste;

    public FagsakRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public FagsakRestTjeneste(FagsakTjeneste fagsakTjeneste, HistorikkinnslagRepository historikkinnslagRepository,
                              FagsakFullTjeneste fagsakFullTjeneste, FagsakEgenskapRepository fagsakEgenskapRepository,
                              ProsessTaskTjeneste taskTjeneste) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.fagsakFullTjeneste = fagsakFullTjeneste;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.taskTjeneste = taskTjeneste;
    }

    @GET
    @Path(STATUS_PART_PATH)
    @Operation(description = "Url for å polle på fagsak mens behandlingprosessen pågår i bakgrunnen(asynkront)", summary = "Returnerer link til enten samme (hvis ikke ferdig) eller redirecter til /fagsak dersom asynkrone operasjoner er ferdig.", tags = "fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Status", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AsyncPollingStatus.class))),
            @ApiResponse(responseCode = "303", description = "Pågående prosesstasks avsluttet", headers = @Header(name = HttpHeaders.LOCATION)),
            @ApiResponse(responseCode = "418", description = "ProsessTasks har feilet", headers = @Header(name = HttpHeaders.LOCATION), content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AsyncPollingStatus.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
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
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
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
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
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
    @Operation(description = "Søk etter saker på saksnummer eller fødselsnummer", tags = "fagsak", summary =
        "Spesifikke saker kan søkes via saksnummer. " + "Oversikt over saker knyttet til en bruker kan søkes via fødselsnummer eller d-nummer.")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public List<FagsakSøkDto> sokFagsaker(@TilpassetAbacAttributt(supplierClass = SøkeFeltAbacDataSupplier.class)
        @Parameter(description = "Søkestreng kan være saksnummer, fødselsnummer eller D-nummer.") @Valid SokefeltDto søkestreng) {
        return fagsakTjeneste.søkFagsakDto(søkestreng.getSearchString().trim());
    }

    public static class SøkeFeltAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (SokefeltDto) obj;
            var attributter = AbacDataAttributter.opprett();
            var søkestring = req.getSearchString() != null ? req.getSearchString().trim() : "";
            if (AktørId.erGyldigAktørId(søkestring)) {
                attributter.leggTil(AppAbacAttributtType.AKTØR_ID, søkestring)
                    .leggTil(AppAbacAttributtType.SAKER_FOR_AKTØR, søkestring);
            } else if (PersonIdent.erGyldigFnr(søkestring)) {
                attributter.leggTil(AppAbacAttributtType.FNR, søkestring);
            } else {
                attributter.leggTil(AppAbacAttributtType.SAKSNUMMER, req.getSearchString());
            }
            return attributter;
        }
    }

    @POST
    @Path(NOTAT_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagre nytt notat for sak", tags = "fagsak", summary = "Lagre nytt notat.")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response lagreFagsakNotat(@TilpassetAbacAttributt(supplierClass = LagreFagsakNotatAbacSupplier.class)
                                     @Parameter(description = "Saksnummer og nytt notat") @Valid LagreFagsakNotatDto notatDto) {
        var fagsak = fagsakTjeneste.hentFagsakForSaksnummer(new Saksnummer(notatDto.saksnummer())).orElse(null);
        if (fagsak != null && !notatDto.notat().isEmpty()) {
            fagsakTjeneste.lagreFagsakNotat(fagsak, notatDto.notat());
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }



    @POST
    @Path(ENDRE_UTLAND_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Endre merking (utland) av sak", tags = "fagsak", summary = "Endre merking fra tidligere verdi.")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public Response endreFagsakMerking(@TilpassetAbacAttributt(supplierClass = EndreUtlandAbacDataSupplier.class)
                                          @Parameter(description = "Saksnummer og markering") @Valid EndreUtlandMarkeringDto endreUtland) {
        var fagsak = fagsakTjeneste.hentFagsakForSaksnummer(new Saksnummer(endreUtland.saksnummer())).orElse(null);
        if (fagsak != null) {
            var eksisterende = fagsakEgenskapRepository.finnFagsakMarkeringer(fagsak.getId());
            var nyeMarkeringer = getMarkeringer(endreUtland);
            // Sjekk om uendret merking (nasjonal er default)
            if (eksisterende.size() == nyeMarkeringer.size() && nyeMarkeringer.containsAll(eksisterende)) {
                return Response.ok().build();
            }
            fagsakEgenskapRepository.lagreAlleFagsakMarkeringer(fagsak.getId(), nyeMarkeringer);
            lagHistorikkInnslag(fagsak, eksisterende, nyeMarkeringer);
            var taskGruppe = new ProsessTaskGruppe();
            // Bytt enhet ved behov for åpne behandlinger - vil sørge for å oppdatere LOS
            var behandlingerSomBytterEnhet = fagsakTjeneste.hentÅpneBehandlinger(fagsak).stream()
                .filter(b -> BehandlendeEnhetTjeneste.sjekkSkalOppdatereEnhet(b, nyeMarkeringer).isPresent())
                .toList();
            behandlingerSomBytterEnhet.stream().map(this::opprettOppdaterEnhetTask).forEach(taskGruppe::addNesteSekvensiell);
            // Oppdater LOS-oppgaver for andre tilfelle av endre saksmerking
            var behandlingBytterEnhetId = behandlingerSomBytterEnhet.stream().map(Behandling::getId).collect(Collectors.toSet());
            fagsakTjeneste.hentBehandlingerMedÅpentAksjonspunkt(fagsak).stream()
                .filter(b -> !behandlingBytterEnhetId.contains(b.getId()))
                .map(this::opprettLosProsessTask).forEach(taskGruppe::addNesteSekvensiell);
            if (!taskGruppe.getTasks().isEmpty()) {
                taskTjeneste.lagre(taskGruppe);
            }
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    private static Set<FagsakMarkering> getMarkeringer(EndreUtlandMarkeringDto dto) {
        return Optional.ofNullable(dto.fagsakMarkeringer()).orElseGet(Set::of);
    }

    private ProsessTaskData opprettLosProsessTask(Behandling behandling) {
        var prosessTaskData = ProsessTaskData.forProsessTask(PubliserBehandlingHendelseTask.class);
        prosessTaskData.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
        prosessTaskData.setProperty(PubliserBehandlingHendelseTask.HENDELSE_TYPE, HendelseForBehandling.AKSJONSPUNKT.name());
        return prosessTaskData;
    }

    private ProsessTaskData opprettOppdaterEnhetTask(Behandling behandling) {
        var prosessTaskData = ProsessTaskData.forProsessTask(OppdaterBehandlendeEnhetTask.class);
        prosessTaskData.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
        return prosessTaskData;
    }

    public static class EndreUtlandAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (EndreUtlandMarkeringDto) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, req.saksnummer());
        }
    }

    public static class LagreFagsakNotatAbacSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (LagreFagsakNotatDto) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, req.saksnummer());
        }
    }

    private void lagHistorikkInnslag(Fagsak fagsak, Collection<FagsakMarkering> eksisterende, Collection<FagsakMarkering> ny) {
        var fraVerdi = eksisterende.stream().map(FagsakMarkering::getNavn).collect(Collectors.joining(","));
        var tilVerdi = ny.stream().map(FagsakMarkering::getNavn).collect(Collectors.joining(","));
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(fagsak.getId())
            .medTittel("Fakta er endret")
            .addLinje(fraTilEquals("Saksmarkering", fraVerdi.isEmpty() ? null : fraVerdi, tilVerdi.isEmpty() ? null : tilVerdi))
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }
}
