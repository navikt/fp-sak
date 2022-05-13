package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.bruker.NavBrukerTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.vedtak.OppdaterFagsakStatusTjeneste;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.KobleFagsakerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.OverstyrDekningsgradDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.SaksnummerJournalpostDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("/forvaltningFagsak")
@ApplicationScoped
@Transactional
public class ForvaltningFagsakRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningFagsakRestTjeneste.class);

    private FagsakRepository fagsakRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingRepository behandlingRepository;
    private PersonopplysningRepository personopplysningRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste;
    private OpprettSakTjeneste opprettSakTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private NavBrukerTjeneste brukerTjeneste;
    private OverstyrDekningsgradTjeneste overstyrDekningsgradTjeneste;

    public ForvaltningFagsakRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningFagsakRestTjeneste(BehandlingRepositoryProvider repositoryProvider,
            ProsessTaskTjeneste taskTjeneste,
            OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste,
            OpprettSakTjeneste opprettSakTjeneste,
            PersoninfoAdapter personinfoAdapter,
            NavBrukerTjeneste brukerTjeneste,
            OverstyrDekningsgradTjeneste overstyrDekningsgradTjeneste,
            FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.taskTjeneste = taskTjeneste;
        this.oppdaterFagsakStatusTjeneste = oppdaterFagsakStatusTjeneste;
        this.overstyrDekningsgradTjeneste = overstyrDekningsgradTjeneste;
        this.opprettSakTjeneste = opprettSakTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.brukerTjeneste = brukerTjeneste;
    }

    @POST
    @Path("/avsluttFagsakUtenBehandling")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Avslutt fagsak uten noen behandlinger", tags = "FORVALTNING-fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Avslutter fagsak.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response avsluttFagsakUtenBehandling(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                                    @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || FagsakStatus.AVSLUTTET.equals(fagsak.getStatus())) {
            LOG.warn("Fagsak allerede avsluttet {}", saksnummer.getVerdi());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        LOG.info("Avslutter fagsak med saksnummer: {} ", saksnummer.getVerdi()); // NOSONAR
        oppdaterFagsakStatusTjeneste.avsluttFagsakUtenAktiveBehandlinger(fagsak);
        return Response.ok().build();
    }

    @POST
    @Path("/stengFagsakForVidereBruk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Stenger fagsak for videre bruk", tags = "FORVALTNING-fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Fagsak stengt.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response stengFagsak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
        @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || !FagsakStatus.AVSLUTTET.equals(fagsak.getStatus())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (!fagsak.erStengt()) {
            LOG.info("Stenger fagsak med saksnummer: {} ", saksnummer.getVerdi()); // NOSONAR
            fagsakRepository.fagsakSkalStengesForBruk(fagsak.getId());
        }
        return Response.ok().build();
    }

    @POST
    @Path("/gjenAapneFagsakForVidereBruk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Gjenåpner fagsak for videre bruk", tags = "FORVALTNING-fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Fagsak stengt.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response gjenaapneFagsak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
        @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || !fagsak.erStengt()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (fagsak.erStengt()) {
            LOG.info("Gjenåpner fagsak med saksnummer: {} ", saksnummer.getVerdi()); // NOSONAR
            fagsakRepository.fagsakSkalGjenåpnesForBruk(fagsak.getId());
        }
        return Response.ok().build();
    }

    @POST
    @Path("/kobleSammenFagsaker")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Kobler sammen angitte fagsaker", tags = "FORVALTNING-fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Fagsaker koblet.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response kobleSammenFagsaker(@BeanParam @Valid KobleFagsakerDto dto) {
        var saksnummer1 = new Saksnummer(dto.getSaksnummer1());
        var saksnummer2 = new Saksnummer(dto.getSaksnummer2());
        var fagsak1 = fagsakRepository.hentSakGittSaksnummer(saksnummer1).orElse(null);
        var fagsak2 = fagsakRepository.hentSakGittSaksnummer(saksnummer2).orElse(null);
        var fagsakRelasjon1 = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak1).orElse(null);
        var fagsakRelasjon2 = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak2).orElse(null);
        if (fagsak1 == null || fagsak2 == null || erFagsakRelasjonKoblet(fagsakRelasjon1) || erFagsakRelasjonKoblet(fagsakRelasjon2) // NOSONAR
                || FagsakStatus.AVSLUTTET.equals(fagsak1.getStatus()) || FagsakStatus.AVSLUTTET.equals(fagsak2.getStatus())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        LOG.info("Kobler sammen fagsaker med saksnummer: {} {}", saksnummer1.getVerdi(), saksnummer2.getVerdi()); // NOSONAR
        var behandlingEn = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak1.getId())
                .orElse(null);
        fagsakRelasjonTjeneste.kobleFagsaker(fagsak1, fagsak2, behandlingEn);
        return Response.ok().build();
    }

    @POST
    @Path("/kobleFraFagsaker")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Kobler fra hverandre angitte fagsaker", tags = "FORVALTNING-fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Fagsaker frakoblet.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response kobleFraFagsaker(@BeanParam @Valid KobleFagsakerDto dto) {
        var saksnummer1 = new Saksnummer(dto.getSaksnummer1());
        var saksnummer2 = new Saksnummer(dto.getSaksnummer2());
        var fagsak1 = fagsakRepository.hentSakGittSaksnummer(saksnummer1).orElse(null);
        var fagsak2 = fagsakRepository.hentSakGittSaksnummer(saksnummer2).orElse(null);
        var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak1).orElse(null);
        if (fagsak1 == null || fagsak2 == null || fagsakRelasjon == null || !erFagsakRelasjonKoblet(fagsakRelasjon) ||
                !fagsakRelasjon.getFagsakNrEn().getId().equals(fagsak1.getId()) ||
                !fagsakRelasjon.getFagsakNrTo().get().getId().equals(fagsak2.getId())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        LOG.info("Kobler fra hverandre fagsaker med saksnummer: {} {}", saksnummer1.getVerdi(), saksnummer2.getVerdi()); // NOSONAR
        fagsakRelasjonTjeneste.fraKobleFagsaker(fagsak1, fagsak2);
        return Response.ok().build();
    }

    private boolean erFagsakRelasjonKoblet(FagsakRelasjon fagsakRelasjon) {
        return fagsakRelasjon != null && fagsakRelasjon.getFagsakNrEn() != null && fagsakRelasjon.getFagsakNrTo().isPresent();
    }

    @POST
    @Path("/overstyrDekningsgrad")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Overstyr dekningsgrad. NB: Dersom det finnes en åpen revurdering som har passert beregning. Bruk Overstyr dekningsgrad først og så hoppTilbakeTil FASTSETT_STP_BER.", tags = "FORVALTNING-fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Dekningsgrad overstyrt."),
            @ApiResponse(responseCode = "204", description = "Dekningsgrad er ikke endret."),
            @ApiResponse(responseCode = "400", description = "Dekningsgrad er ugyldig."),
            @ApiResponse(responseCode = "404", description = "Fagsak finnes ikke."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public Response overstyrDekningsgrad(@BeanParam @Valid OverstyrDekningsgradDto dto) {
        return overstyrDekningsgradTjeneste.overstyr(dto.getSaksnummer(), Integer.parseInt(dto.getDekningsgrad()));
    }

    @POST
    @Path("/fagsak/flyttJournalpostFagsak")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Knytt journalpost til fagsak. Før en journalpost journalføres på en fagsak skal fagsaken oppdateres med journalposten.", tags = "FORVALTNING-fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Task satt til ferdig."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response flyttJournalpostTilFagsak(@BeanParam @Valid SaksnummerJournalpostDto dto) {
        var journalpostId = new JournalpostId(dto.getJournalpostId());
        var saksnummer = new Saksnummer(dto.getSaksnummer());
        opprettSakTjeneste.flyttJournalpostTilSak(journalpostId, saksnummer);
        return Response.ok().build();
    }

    @POST
    @Path("/fagsak/oppdaterAktoerId")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Henter ny aktørid for bruker og oppdaterer nødvendige tabeller", tags = "FORVALTNING-fagsak", responses = {
            @ApiResponse(responseCode = "200", description = "Task satt til ferdig."),
            @ApiResponse(responseCode = "400", description = "AktørId er uendret."),
            @ApiResponse(responseCode = "400", description = "Saksnummer er ugyldig."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public Response oppdaterAktoerId(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
        @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || fagsak.erStengt()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var eksisterendeAktørId = fagsak.getAktørId();
        var gjeldendeAktørId = personinfoAdapter.hentFnr(fagsak.getAktørId()).flatMap(personinfoAdapter::hentAktørForFnr)
            .orElseThrow(() -> new IllegalStateException("Kan ikke mappe aktørId - ident - aktørId" + fagsak.getAktørId()));
        if (gjeldendeAktørId.equals(eksisterendeAktørId)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var brukerForGjeldendeAktørId = brukerTjeneste.hentBrukerForAktørId(gjeldendeAktørId);
        if (brukerForGjeldendeAktørId.isPresent()) {
            fagsakRepository.oppdaterBruker(fagsak.getId(), brukerForGjeldendeAktørId.orElse(null));
        } else {
            fagsakRepository.oppdaterBrukerMedAktørId(fagsak.getNavBruker().getId(), gjeldendeAktørId);
        }
        personopplysningRepository.oppdaterAktørIdFor(eksisterendeAktørId, gjeldendeAktørId);
        return Response.ok().build();
    }

    @POST
    @Path("/fagsak/setStatusUnderBehandlingFagsak")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Sett riktig status for fagsak", tags = "FORVALTNING-fagsak", responses = {
        @ApiResponse(responseCode = "200", description = "Oppdatert fagsak.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public Response setStatusUnderBehandlingFagsak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                                @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || FagsakStatus.UNDER_BEHANDLING.equals(fagsak.getStatus()) || FagsakStatus.AVSLUTTET.equals(fagsak.getStatus())) {
            LOG.warn("Ugyldig fagsak {}", saksnummer.getVerdi());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        LOG.info("Avslutter fagsak med saksnummer: {} ", saksnummer.getVerdi()); // NOSONAR
        oppdaterFagsakStatusTjeneste.settUnderBehandlingNårAktiveBehandlinger(fagsak);
        return Response.ok().build();
    }
}
