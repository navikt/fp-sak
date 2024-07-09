package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.EmptyStackException;
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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
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
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.kontrakter.fordel.YtelseTypeDto;
import no.nav.foreldrepenger.produksjonsstyring.fagsakstatus.OppdaterFagsakStatusTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.OpprettSakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.KobleFagsakerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.SaksnummerJournalpostDto;
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

    public ForvaltningFagsakRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningFagsakRestTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                         OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste,
                                         OpprettSakTjeneste opprettSakTjeneste,
                                         AktørTjeneste aktørTjeneste,
                                         NavBrukerTjeneste brukerTjeneste,
                                         FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.oppdaterFagsakStatusTjeneste = oppdaterFagsakStatusTjeneste;
        this.opprettSakTjeneste = opprettSakTjeneste;
        this.aktørTjeneste = aktørTjeneste;
        this.brukerTjeneste = brukerTjeneste;
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
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK)
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
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK)
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
    @Path("/gjenAapneFagsakForVidereBruk")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Gjenåpner fagsak for videre bruk", tags = "FORVALTNING-fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Fagsak stengt.", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK)
    public Response gjenaapneFagsak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
        @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || !fagsak.erStengt()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (fagsak.erStengt()) {
            LOG.info("Gjenåpner fagsak med saksnummer: {} ", saksnummer.getVerdi());
            fagsakRepository.fagsakSkalGjenåpnesForBruk(fagsak.getId());
        }
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
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK)
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
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK)
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
    @Path("/fagsak/flyttJournalpostFagsak")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Knytt journalpost til fagsak. Før en journalpost journalføres på en fagsak skal fagsaken oppdateres med journalposten.", tags = "FORVALTNING-fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Task satt til ferdig."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK)
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
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
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
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
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
