package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.DRIFT;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.foreldrepenger.domene.vedtak.intern.SettFagsakRelasjonAvslutningsdatoTask;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.HenleggFlyttFagsakTask;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.vedtak.OppdaterFagsakStatus;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.KobleFagsakerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.OverstyrDekningsgradDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.SaksnummerJournalpostDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("/forvaltningFagsak")
@ApplicationScoped
@Transactional
public class ForvaltningFagsakRestTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ForvaltningFagsakRestTjeneste.class);

    private FagsakRepository fagsakRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingRepository behandlingRepository;
    private PersonopplysningRepository personopplysningRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private Instance<OppdaterFagsakStatus> oppdaterFagsakStatuser;
    private OpprettSakTjeneste opprettSakTjeneste;
    private OverstyrDekningsgradTjeneste overstyrDekningsgradTjeneste;


    public ForvaltningFagsakRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningFagsakRestTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                         ProsessTaskRepository prosessTaskRepository,
                                         @Any Instance<OppdaterFagsakStatus> oppdaterFagsakStatuser,
                                         OpprettSakTjeneste opprettSakTjeneste,
                                         OverstyrDekningsgradTjeneste overstyrDekningsgradTjeneste,
                                         FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.prosessTaskRepository = prosessTaskRepository;
        this.oppdaterFagsakStatuser = oppdaterFagsakStatuser;
        this.overstyrDekningsgradTjeneste = overstyrDekningsgradTjeneste;
        this.opprettSakTjeneste = opprettSakTjeneste;
    }

    @POST
    @Path("/avsluttFagsakUtenBehandling")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Avslutt fagsak uten noen behandlinger",
        tags = "FORVALTNING-fagsak",
        responses = {
            @ApiResponse(responseCode = "200", description = "Avslutter fagsak.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)
                )
            ),
            @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response avsluttFagsakUtenBehandling(@NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        Saksnummer saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || FagsakStatus.AVSLUTTET.equals(fagsak.getStatus())) {
            ForvaltningRestTjenesteFeil.FACTORY.ugyldigeSakStatus(saksnummer.getVerdi()).log(logger);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else {
            logger.info("Avslutter fagsak med saksnummer: {} ", saksnummer.getVerdi()); //NOSONAR
            OppdaterFagsakStatus oppdaterFagsakStatus = FagsakYtelseTypeRef.Lookup.find(oppdaterFagsakStatuser, fagsak.getYtelseType())
                .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + fagsak.getYtelseType().getKode()));
            oppdaterFagsakStatus.avsluttFagsakUtenAktiveBehandlinger(fagsak);
            return Response.ok().build();
        }
    }

    @POST
    @Path("/revurderAvslutningForFagsaker")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Revurder avslutningsdato for fagsaker",
        tags = "FORVALTNING-fagsak",
        responses = {
            @ApiResponse(responseCode = "200", description = "Revurderer avslutning av fagsaker.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)
                )
            ),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response revurderAvslutningForFagsaker(@NotNull @Valid AbacSaksnummerListe saksnumre) {

        for(String abacSaksnummer: saksnumre.get().orElseThrow()) {
            Saksnummer saksnummer = new Saksnummer(abacSaksnummer);
            Optional<Fagsak> fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
            if (fagsak.isEmpty() || FagsakStatus.AVSLUTTET == fagsak.get().getStatus()) {
                ForvaltningRestTjenesteFeil.FACTORY.ugyldigeSakStatus(saksnummer.getVerdi()).log(logger);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            ProsessTaskGruppe taskGruppe = new ProsessTaskGruppe();
            Optional<Behandling> behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.orElseThrow().getId());
            taskGruppe.setBehandling(fagsak.orElseThrow().getId(), behandling.orElseThrow().getId(), behandling.orElseThrow().getAktørId().getId());

            ProsessTaskData task = new ProsessTaskData(SettFagsakRelasjonAvslutningsdatoTask.TASKTYPE);
            task.setFagsakId(fagsak.orElseThrow().getId());
            task.setPrioritet(50);
            taskGruppe.addNesteSekvensiell(task);

            prosessTaskRepository.lagre(taskGruppe);
        }
        return Response.ok().build();
    }

    @JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
    public static class AbacSaksnummerListe implements AbacDto {

        @JsonProperty("saksnumre")
        List<String> saksnumre;
        public AbacSaksnummerListe() {
            // for Jackson
        }

        public AbacSaksnummerListe(List<String> saksnumre) {
            this.saksnumre = saksnumre;
        }

        public Optional<List<String>> get() {
            return Optional.ofNullable(saksnumre);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, get());
        }

    }

    @POST
    @Path("/flyttFagsakTilInfotrygd")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Steng fagsak og flytt til Infotrygd",
        tags = "FORVALTNING-fagsak",
        responses = {
            @ApiResponse(responseCode = "200", description = "Flyttet fagsak.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)
                )
            ),
            @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response flyttFagsakTilInfotrygd(@NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        Saksnummer saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || FagsakStatus.LØPENDE.equals(fagsak.getStatus()) || FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else if (FagsakStatus.AVSLUTTET.equals(fagsak.getStatus())) {
            if (!fagsak.getSkalTilInfotrygd()) {
                logger.info("Flagger Infotrygd for fagsak med saksnummer: {} ", saksnummer.getVerdi()); //NOSONAR
                fagsakRepository.fagsakSkalBehandlesAvInfotrygd(fagsak.getId());
            }
            return Response.ok().build();
        } else {
            List<Behandling> behandlinger = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId());
            if (behandlinger.isEmpty()) {
                Response avslutning = avsluttFagsakUtenBehandling(saksnummerDto);
                if (Response.Status.OK.equals(avslutning.getStatusInfo()) && !fagsak.getSkalTilInfotrygd()) {
                    logger.info("Flagger Infotrygd for fagsak med saksnummer: {} ", saksnummer.getVerdi()); //NOSONAR
                    fagsakRepository.fagsakSkalBehandlesAvInfotrygd(fagsak.getId());
                } else {
                    return avslutning;
                }
            } else {
                logger.info("Henlegger behandlinger og flagger Infotrygd for fagsak med saksnummer: {} ", saksnummer.getVerdi()); //NOSONAR
                behandlinger.forEach(behandling -> opprettHenleggelseTask(behandling, BehandlingResultatType.MANGLER_BEREGNINGSREGLER));
            }
            return Response.ok().build();
        }
    }

    private void opprettHenleggelseTask(Behandling behandling, BehandlingResultatType henleggelseType) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(HenleggFlyttFagsakTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setProperty(HenleggFlyttFagsakTask.HENLEGGELSE_TYPE_KEY, henleggelseType.getKode());
        prosessTaskData.setCallIdFraEksisterende();

        prosessTaskRepository.lagre(prosessTaskData);
    }

    @POST
    @Path("/stengFagsakForVidereBruk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Stenger fagsak for videre bruk",
        tags = "FORVALTNING-fagsak",
        responses = {
            @ApiResponse(responseCode = "200", description = "Fagsak stengt.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)
                )
            ),
            @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response stengFagsak(@NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        Saksnummer saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || !FagsakStatus.AVSLUTTET.equals(fagsak.getStatus())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (!fagsak.getSkalTilInfotrygd()) {
            logger.info("Stenger fagsak med saksnummer: {} ", saksnummer.getVerdi()); //NOSONAR
            fagsakRepository.fagsakSkalBehandlesAvInfotrygd(fagsak.getId());
        }
        return Response.ok().build();
    }

    @POST
    @Path("/gjenAapneFagsakForVidereBruk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Gjenåpner fagsak for videre bruk",
        tags = "FORVALTNING-fagsak",
        responses = {
            @ApiResponse(responseCode = "200", description = "Fagsak stengt.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)
                )
            ),
            @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response gjenaapneFagsak(@NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        Saksnummer saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || !fagsak.getSkalTilInfotrygd()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (fagsak.getSkalTilInfotrygd()) {
            logger.info("Gjenåpner fagsak med saksnummer: {} ", saksnummer.getVerdi()); //NOSONAR
            fagsakRepository.fagsakSkalGjenåpnesForBruk(fagsak.getId());
        }
        return Response.ok().build();
    }

    @POST
    @Path("/kobleSammenFagsaker")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Kobler sammen angitte fagsaker",
        tags = "FORVALTNING-fagsak",
        responses = {
            @ApiResponse(responseCode = "200", description = "Fagsaker koblet.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)
                )
            ),
            @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response kobleSammenFagsaker(@BeanParam @Valid KobleFagsakerDto dto) {
        Saksnummer saksnummer1 = new Saksnummer(dto.getSaksnummer1());
        Saksnummer saksnummer2 = new Saksnummer(dto.getSaksnummer2());
        Fagsak fagsak1 = fagsakRepository.hentSakGittSaksnummer(saksnummer1).orElse(null);
        Fagsak fagsak2 = fagsakRepository.hentSakGittSaksnummer(saksnummer2).orElse(null);
        FagsakRelasjon fagsakRelasjon1 = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak1).orElse(null);
        FagsakRelasjon fagsakRelasjon2 = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak2).orElse(null);
        if (fagsak1 == null || fagsak2 == null || erFagsakRelasjonKoblet(fagsakRelasjon1) || erFagsakRelasjonKoblet(fagsakRelasjon2) //NOSONAR
            || FagsakStatus.AVSLUTTET.equals(fagsak1.getStatus()) || FagsakStatus.AVSLUTTET.equals(fagsak2.getStatus())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else {
            logger.info("Kobler sammen fagsaker med saksnummer: {} {}", saksnummer1.getVerdi(), saksnummer2.getVerdi()); //NOSONAR
            Behandling behandlingEn = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak1.getId())
                .orElse(null);
            fagsakRelasjonTjeneste.kobleFagsaker(fagsak1, fagsak2, behandlingEn);
            return Response.ok().build();
        }
    }

    @POST
    @Path("/kobleFraFagsaker")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Kobler fra hverandre angitte fagsaker",
        tags = "FORVALTNING-fagsak",
        responses = {
            @ApiResponse(responseCode = "200", description = "Fagsaker frakoblet.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = String.class)
                )
            ),
            @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response kobleFraFagsaker(@BeanParam @Valid KobleFagsakerDto dto) {
        Saksnummer saksnummer1 = new Saksnummer(dto.getSaksnummer1());
        Saksnummer saksnummer2 = new Saksnummer(dto.getSaksnummer2());
        Fagsak fagsak1 = fagsakRepository.hentSakGittSaksnummer(saksnummer1).orElse(null);
        Fagsak fagsak2 = fagsakRepository.hentSakGittSaksnummer(saksnummer2).orElse(null);
        FagsakRelasjon fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak1).orElse(null);
        if (fagsak1 == null || fagsak2 == null || fagsakRelasjon == null || !erFagsakRelasjonKoblet(fagsakRelasjon) ||
            !fagsakRelasjon.getFagsakNrEn().getId().equals(fagsak1.getId()) ||
            !fagsakRelasjon.getFagsakNrTo().get().getId().equals(fagsak2.getId())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else {
            logger.info("Kobler fra hverandre fagsaker med saksnummer: {} {}", saksnummer1.getVerdi(), saksnummer2.getVerdi()); //NOSONAR
            fagsakRelasjonTjeneste.fraKobleFagsaker(fagsak1, fagsak2);
            return Response.ok().build();
        }
    }

    private boolean erFagsakRelasjonKoblet(FagsakRelasjon fagsakRelasjon) {
        return fagsakRelasjon != null && fagsakRelasjon.getFagsakNrEn() != null && fagsakRelasjon.getFagsakNrTo().isPresent();
    }

    @POST
    @Path("/overstyrDekningsgrad")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Overstyr dekningsgrad. NB: Dersom det finnes en åpen revurdering som har passert beregning. Bruk Overstyr dekningsgrad først og så hoppTilbakeTil FASTSETT_STP_BER.",
        tags = "FORVALTNING-fagsak",
        responses = {
            @ApiResponse(responseCode = "200", description = "Dekningsgrad overstyrt."),
            @ApiResponse(responseCode = "204", description = "Dekningsgrad er ikke endret."),
            @ApiResponse(responseCode = "400", description = "Dekningsgrad er ugyldig."),
            @ApiResponse(responseCode = "404", description = "Fagsak finnes ikke."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response overstyrDekningsgrad(@BeanParam @Valid OverstyrDekningsgradDto dto) {
        return overstyrDekningsgradTjeneste.overstyr(dto.getSaksnummer(), Integer.parseInt(dto.getDekningsgrad()));
    }

    @POST
    @Path("/fagsak/flyttJournalpostFagsak")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Knytt journalpost til fagsak. Før en journalpost journalføres på en fagsak skal fagsaken oppdateres med journalposten.",
        tags = "FORVALTNING-fagsak",
        responses = {
            @ApiResponse(responseCode = "200", description = "Task satt til ferdig."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = FAGSAK)
    public Response flyttJournalpostTilFagsak(@BeanParam @Valid SaksnummerJournalpostDto dto) {
        JournalpostId journalpostId = new JournalpostId(dto.getJournalpostId());
        Saksnummer saksnummer = new Saksnummer(dto.getSaksnummer());
        opprettSakTjeneste.flyttJournalpostTilSak(journalpostId, saksnummer);
        return Response.ok().build();
    }

    @POST
    @Path("/fagsak/oppdaterAktoerId")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Henter ny aktørid for bruker og oppdaterer nødvendige tabeller",
        tags = "FORVALTNING-fagsak",
        responses = {
            @ApiResponse(responseCode = "200", description = "Task satt til ferdig."),
            @ApiResponse(responseCode = "400", description = "AktørId er uendret."),
            @ApiResponse(responseCode = "400", description = "Saksnummer er ugyldig."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
        })
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT)
    public Response oppdaterAktoerId(@NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        Saksnummer saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null || fagsak.getSkalTilInfotrygd()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var eksisterendeAktørId = fagsak.getAktørId();
        var gjeldendeAktørId = opprettSakTjeneste.hentGjeldendeAktørId(fagsak.getAktørId());
        if (gjeldendeAktørId.equals(eksisterendeAktørId)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var brukerForGjeldendeAktørId = opprettSakTjeneste.hentNavBrukerFor(gjeldendeAktørId);
        if (brukerForGjeldendeAktørId.isPresent()) {
            fagsakRepository.oppdaterBruker(fagsak.getId(), brukerForGjeldendeAktørId.orElse(null));
        } else {
            fagsakRepository.oppdaterBrukerMedAktørId(fagsak.getNavBruker().getId(), gjeldendeAktørId);
        }
        personopplysningRepository.oppdaterAktørIdFor(eksisterendeAktørId, gjeldendeAktørId);
        return Response.ok().build();
    }
}
