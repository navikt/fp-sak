package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.bruker.NavBrukerTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.AktørTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.kontrakter.fordel.YtelseTypeDto;
import no.nav.foreldrepenger.produksjonsstyring.fagsakstatus.OppdaterFagsakStatusTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SokefeltDto;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.OpprettSakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.KobleFagsakerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.SaksnummerBrukerRolleDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.SaksnummerJournalpostDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.foreldrepenger.web.server.abac.InvaliderSakPersonCacheKlient;
import no.nav.pdl.HentIdenterQueryRequest;
import no.nav.pdl.IdentGruppe;
import no.nav.pdl.IdentInformasjonResponseProjection;
import no.nav.pdl.IdentlisteResponseProjection;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.integrasjon.person.Persondata;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltningFagsak")
@ApplicationScoped
@Transactional
public class ForvaltningFagsakRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningFagsakRestTjeneste.class);

    private FagsakRepository fagsakRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private PersonopplysningRepository personopplysningRepository;
    private OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste;
    private OpprettSakTjeneste opprettSakTjeneste;
    private AktørTjeneste aktørTjeneste;
    private NavBrukerTjeneste brukerTjeneste;
    private Persondata pdlKlient;
    private BehandlingRepository behandlingRepository;
    private InvaliderSakPersonCacheKlient invaliderSakPersonCacheKlient;

    public ForvaltningFagsakRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningFagsakRestTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                         OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste,
                                         OpprettSakTjeneste opprettSakTjeneste,
                                         AktørTjeneste aktørTjeneste,
                                         NavBrukerTjeneste brukerTjeneste,
                                         FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                         Persondata pdlKlient, InvaliderSakPersonCacheKlient invaliderSakPersonCacheKlient) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.oppdaterFagsakStatusTjeneste = oppdaterFagsakStatusTjeneste;
        this.opprettSakTjeneste = opprettSakTjeneste;
        this.aktørTjeneste = aktørTjeneste;
        this.brukerTjeneste = brukerTjeneste;
        this.pdlKlient = pdlKlient;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.invaliderSakPersonCacheKlient = invaliderSakPersonCacheKlient;
    }

    @POST
    @Path("/avsluttFagsakUtenBehandling")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Avslutt fagsak uten noen behandlinger", tags = "FORVALTNING-fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Avslutter fagsak.", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response avsluttFagsakUtenBehandling(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                                    @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || FagsakStatus.AVSLUTTET.equals(fagsak.getStatus())) {
            LOG.warn("Fagsak allerede avsluttet {}", saksnummer.getVerdi());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        LOG.info("Avslutter fagsak med saksnummer: {} ", saksnummer.getVerdi());
        oppdaterFagsakStatusTjeneste.avsluttFagsakUtenAktiveBehandlinger(fagsak);
        return Response.ok().build();
    }

    @POST
    @Path("/stengFagsakForVidereBruk")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Stenger fagsak for videre bruk", tags = "FORVALTNING-fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Fagsak stengt.", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response stengFagsak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
        @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || !FagsakStatus.AVSLUTTET.equals(fagsak.getStatus())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (!fagsak.erStengt()) {
            LOG.info("Stenger fagsak med saksnummer: {} ", saksnummer.getVerdi());
            fagsakRepository.fagsakSkalStengesForBruk(fagsak.getId());
        }
        return Response.ok().build();
    }

    @POST
    @Path("/settFagsakFraAvsluttetTilUnderBehandling")
    @Operation(description = "Setter status for fagsak fra avsluttet til under behandling", tags = "FORVALTNING-fagsak", responses = {
        @ApiResponse(responseCode = "200", description = "Fagsak endret.", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt, eller fagsak i feil tilstand"),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response settFagsakFraAvsluttetTilUnderBehandling(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                    @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || !fagsak.getStatus().equals(FagsakStatus.AVSLUTTET)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsak.getId()).isEmpty()) {
            LOG.info("Fagsak har ingen åpne behandlinger for fagsak");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        LOG.info("Setter fagsakstatus til under behandling");
        fagsakRepository.oppdaterFagsakStatus(fagsak.getId(), FagsakStatus.UNDER_BEHANDLING);
        return Response.ok().build();
    }

    @POST
    @Path("/kobleSammenFagsaker")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Kobler sammen angitte fagsaker", tags = "FORVALTNING-fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Fagsaker koblet.", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response kobleSammenFagsaker(@BeanParam @Valid KobleFagsakerDto dto) {
        var saksnummer1 = new Saksnummer(dto.getSaksnummer1());
        var saksnummer2 = new Saksnummer(dto.getSaksnummer2());
        var fagsak1 = fagsakRepository.hentSakGittSaksnummer(saksnummer1).orElse(null);
        var fagsak2 = fagsakRepository.hentSakGittSaksnummer(saksnummer2).orElse(null);
        var fagsakRelasjon1 = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak1).orElse(null);
        var fagsakRelasjon2 = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak2).orElse(null);
        if (fagsak1 == null || fagsak2 == null || erFagsakRelasjonKoblet(fagsakRelasjon1) || erFagsakRelasjonKoblet(fagsakRelasjon2)
                || FagsakStatus.AVSLUTTET.equals(fagsak1.getStatus()) || FagsakStatus.AVSLUTTET.equals(fagsak2.getStatus())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        LOG.info("Kobler sammen fagsaker med saksnummer: {} {}", saksnummer1.getVerdi(), saksnummer2.getVerdi());
        fagsakRelasjonTjeneste.kobleFagsaker(fagsak1, fagsak2);
        return Response.ok().build();
    }

    @POST
    @Path("/kobleFraFagsaker")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Kobler fra hverandre angitte fagsaker", tags = "FORVALTNING-fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Fagsaker frakoblet.", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response kobleFraFagsaker(@BeanParam @Valid KobleFagsakerDto dto) {
        var saksnummer1 = new Saksnummer(dto.getSaksnummer1());
        var saksnummer2 = new Saksnummer(dto.getSaksnummer2());
        var fagsak1 = fagsakRepository.hentSakGittSaksnummer(saksnummer1).orElse(null);
        var fagsak2 = fagsakRepository.hentSakGittSaksnummer(saksnummer2).orElse(null);
        var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak1).orElse(null);
        if (fagsak1 == null || fagsak2 == null || !erFagsakRelasjonKoblet(fagsakRelasjon) || !fagsakRelasjon.getFagsakNrEn()
            .getId()
            .equals(fagsak1.getId()) || !fagsakRelasjon.getFagsakNrTo().orElseThrow().getId().equals(fagsak2.getId())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        LOG.info("Kobler fra hverandre fagsaker med saksnummer: {} {}", saksnummer1.getVerdi(), saksnummer2.getVerdi());
        fagsakRelasjonTjeneste.fraKobleFagsaker(fagsak1, fagsak2);
        return Response.ok().build();
    }

    private boolean erFagsakRelasjonKoblet(FagsakRelasjon fagsakRelasjon) {
        return fagsakRelasjon != null && fagsakRelasjon.getFagsakNrEn() != null && fagsakRelasjon.getFagsakNrTo().isPresent();
    }

    @POST
    @Path("/endreSaksrolle")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Oppdaterer brukes rolle i fagsaken", tags = "FORVALTNING-fagsak", responses = {
        @ApiResponse(responseCode = "200", description = "Fagsaker frakoblet.", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response endreSaksrolle(@BeanParam @Valid SaksnummerBrukerRolleDto dto) {
        var saksnummer = new Saksnummer(dto.getSaksnummer());
        var responstekst = "Saksrolle endret.";
        var nyRolle = switch (dto.getRolle()) {
            case MOR -> no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType.MORA;
            case FAR -> no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType.FARA;
            case MEDMOR -> no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType.MEDMOR;
        };
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow();
        var åpnebehandlinger = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsak.getId());
        if (åpnebehandlinger.stream().anyMatch(SpesialBehandling::erSpesialBehandling)) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "Åpen berørt behandling").build();
        } else if (!åpnebehandlinger.isEmpty()) {
            responstekst = responstekst + " Åpen behandling - hopp tilbake til KOARB pga STP";
        }  else {
            responstekst = responstekst + " Opprett revurdering fra meny.";
        }
        fagsakRepository.oppdaterRelasjonsRolle(fagsak.getId(), nyRolle);
        LOG.info("Brukerrolle sak {} oppdatert til {}", saksnummer.getVerdi(), nyRolle);
        return Response.ok(responstekst).build();
    }

    @POST
    @Path("/fagsak/flyttJournalpostFagsak")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Knytt journalpost til fagsak. Før en journalpost journalføres på en fagsak skal fagsaken oppdateres med journalposten.", tags = "FORVALTNING-fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Task satt til ferdig."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response flyttJournalpostTilFagsak(@BeanParam @Valid SaksnummerJournalpostDto dto) {
        var journalpostId = new JournalpostId(dto.getJournalpostId());
        var saksnummer = new Saksnummer(dto.getSaksnummer());
        opprettSakTjeneste.flyttJournalpostTilSak(journalpostId, saksnummer);
        return Response.ok().build();
    }

    @POST
    @Path("/fagsak/oppdaterAktoerIdFraPdl")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Henter ny aktørid for bruker og oppdaterer nødvendige tabeller", tags = "FORVALTNING-fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Task satt til ferdig."),
            @ApiResponse(responseCode = "400", description = "AktørId er uendret."),
            @ApiResponse(responseCode = "400", description = "Saksnummer er ugyldig."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response oppdaterAktoerIdFraPdl(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
        @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || fagsak.erStengt()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var eksisterendeAktørId = fagsak.getAktørId();
        var gjeldendeAktørId = aktørTjeneste.hentPersonIdentForAktørIdNonCached(fagsak.getAktørId())
            .flatMap(aktørTjeneste::hentAktørIdForPersonIdentNonCached)
            .orElseThrow(() -> new IllegalStateException("Kan ikke mappe aktørId - ident - aktørId" + fagsak.getAktørId()));
        if (gjeldendeAktørId.equals(eksisterendeAktørId)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var brukerForGjeldendeAktørId = brukerTjeneste.hentBrukerForAktørId(gjeldendeAktørId);
        if (brukerForGjeldendeAktørId.isPresent()) {
            fagsakRepository.oppdaterBruker(fagsak.getId(), brukerForGjeldendeAktørId.orElse(null));
        } else {
            fagsakRepository.oppdaterBrukerMedAktørId(fagsak.getId(), gjeldendeAktørId);
        }
        personopplysningRepository.oppdaterAktørIdFor(eksisterendeAktørId, gjeldendeAktørId);
        return Response.ok().build();
    }

    @POST
    @Path("/fagsak/oppdaterAktoerId")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Setter ny aktørid for bruker og oppdaterer nødvendige tabeller", tags = "FORVALTNING-fagsak", responses = {
        @ApiResponse(responseCode = "200", description = "Task satt til ferdig."),
        @ApiResponse(responseCode = "400", description = "AktørId er uendret."),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response oppdaterAktoerId(@TilpassetAbacAttributt(supplierClass = ByttAktørRequestAbacDataSupplier.class)
                                         @NotNull @Valid ByttAktørRequestDto dto) {
        if (dto.gyldigAktørId().equals(dto.utgåttAktørId())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        fagsakRepository.oppdaterBrukerMedAktørId(dto.utgåttAktørId(), dto.gyldigAktørId());
        personopplysningRepository.oppdaterAktørIdFor(dto.utgåttAktørId(), dto.gyldigAktørId());
        return Response.ok().build();
    }

    @GET
    @Path("/fagsak/{fagsakId}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Finner fagsak informasjon for et gitt fagsakId", tags = "FORVALTNING-fagsak")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response hentFagsakInformasjon(@TilpassetAbacAttributt(supplierClass = AbacEmptySupplier.class) @PathParam("fagsakId") @Valid Long fagsakId) {
        if (fagsakId == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var forvaltningInfoDto = mapTilDto(fagsakRepository.finnEksaktFagsakReadOnly(fagsakId));
        return Response.ok(forvaltningInfoDto).build();
    }

    @POST
    @Path("/fagsak/identhistorikk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Finner identhistorikk gitt en aktørId eller personident", tags = "FORVALTNING-fagsak")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response identhistorikk(@TilpassetAbacAttributt(supplierClass = SøkeFeltAbacDataSupplier.class)
                                 @Parameter(description = "Søkestreng kan være aktørId, fødselsnummer eller D-nummer.") @Valid SokefeltDto søkestreng) {
        var trimmed = søkestreng.getSearchString();
        var ident = PersonIdent.erGyldigFnr(trimmed) || AktørId.erGyldigAktørId(trimmed) ? trimmed : null;
        if (ident == null) {
            return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).build();
        }
        return Response.ok(finnAlleHistoriskeFødselsnummer(ident)).build();
    }

    @POST
    @Path("/fagsak/oppdater-personer-tilgang")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Avslutt fagsak uten noen behandlinger", tags = "FORVALTNING-fagsak", responses = {
        @ApiResponse(responseCode = "200", description = "Avslutter fagsak.", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response oppdaterPersongalleriForTilgang(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                                        @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow();
        invaliderSakPersonCacheKlient.invaliderSakCache(saksnummer);
        return Response.ok().build();
    }

    private List<String> finnAlleHistoriskeFødselsnummer(String inputIdent) {
        var request = new HentIdenterQueryRequest();
        request.setIdent(inputIdent);
        request.setGrupper(List.of(IdentGruppe.FOLKEREGISTERIDENT, IdentGruppe.NPID, IdentGruppe.AKTORID));
        request.setHistorikk(Boolean.TRUE);
        var projection = new IdentlisteResponseProjection()
            .identer(new IdentInformasjonResponseProjection().ident());

        try {
            var identliste = pdlKlient.hentIdenter(request, projection);
            return identliste.getIdenter().stream().map(i -> i.getIdent() + (i.getHistorisk() ? "H" : "A")).toList();
        } catch (VLException v) {
            if (Persondata.PDL_KLIENT_NOT_FOUND_KODE.equals(v.getKode())) {
                return List.of();
            }
            throw v;
        } catch (ProcessingException e) {
            throw e.getCause() instanceof SocketTimeoutException ? new IntegrasjonException("FP-723618", "PDL timeout") : e;
        }
    }

    public static class SøkeFeltAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (SokefeltDto) obj;
            var attributter = AbacDataAttributter.opprett();
            var søkestring = req.getSearchString();
            if (søkestring.length() == 13 /* guess - aktørId */) {
                attributter.leggTil(AppAbacAttributtType.AKTØR_ID, søkestring);
            } else if (søkestring.length() == 11 /* guess - FNR */) {
                attributter.leggTil(AppAbacAttributtType.FNR, søkestring);
            }
            return attributter;
        }
    }

    public static class AbacEmptySupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }

    private FagsakForvaltningInfoDto mapTilDto(Fagsak fagsak) {
        return new FagsakForvaltningInfoDto(fagsak.getId(), new SaksnummerDto(fagsak.getSaksnummer()), mapYtelseType(fagsak.getYtelseType()));
    }

    private YtelseTypeDto mapYtelseType(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> YtelseTypeDto.FORELDREPENGER;
            case ENGANGSTØNAD -> YtelseTypeDto.ENGANGSTØNAD;
            case SVANGERSKAPSPENGER -> YtelseTypeDto.SVANGERSKAPSPENGER;
            case UDEFINERT -> null;
        };
    }

    record FagsakForvaltningInfoDto(@NotNull Long fagsakId, @NotNull @Valid SaksnummerDto saksnummer, @NotNull YtelseTypeDto ytelseType) {
    }

    /**
     * Input request for å bytte en utgått aktørid med en aktiv
     */
    public record ByttAktørRequestDto(@NotNull @Valid AktørId utgåttAktørId,
                                      @NotNull @Valid AktørId gyldigAktørId) { }

    public static class ByttAktørRequestAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        public ByttAktørRequestAbacDataSupplier() {
            // Jackson
        }

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (ByttAktørRequestDto) obj;
            return AbacDataAttributter.opprett()
                .leggTil(StandardAbacAttributtType.AKTØR_ID, req.utgåttAktørId().getId())
                .leggTil(StandardAbacAttributtType.AKTØR_ID, req.gyldigAktørId().getId());
        }
    }
}
