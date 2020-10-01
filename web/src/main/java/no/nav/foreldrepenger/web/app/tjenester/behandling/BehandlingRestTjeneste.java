package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static no.nav.vedtak.feil.LogLevel.ERROR;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsoppretterApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingRettigheterDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.ByttBehandlendeEnhetDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.GjenopptaBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.HenleggBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.NyBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.Redirect;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.ReåpneBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.SettBehandlingPaVentDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.AnnenPartBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.ProsessTaskGruppeIdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.UtvidetBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.FunksjonellFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.felles.jpa.TomtResultatException;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@ApplicationScoped
@Transactional
@Path(BehandlingRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class BehandlingRestTjeneste {

    static final String BASE_PATH = "/behandlinger";
    private static final String ANNEN_PART_BEHANDLING_PART_PATH = "/annen-part-behandling";
    public static final String ANNEN_PART_BEHANDLING_PATH = BASE_PATH + ANNEN_PART_BEHANDLING_PART_PATH; // NOSONAR TFP-2234
    private static final String BEHANDLINGER_ALLE_PART_PATH = "/alle";
    public static final String BEHANDLINGER_ALLE_PATH = BASE_PATH + BEHANDLINGER_ALLE_PART_PATH; // NOSONAR TFP-2234
    private static final String BEHANDLINGER_PART_PATH = "";
    public static final String BEHANDLINGER_PATH = BASE_PATH + BEHANDLINGER_PART_PATH; // NOSONAR TFP-2234
    private static final String BEHANDLINGER_STATUS_PART_PATH = "/status";
    public static final String BEHANDLINGER_STATUS_PATH = BASE_PATH + BEHANDLINGER_STATUS_PART_PATH; // NOSONAR TFP-2234
    private static final String BYTT_ENHET_PART_PATH = "/bytt-enhet";
    public static final String BYTT_ENHET_PATH = BASE_PATH + BYTT_ENHET_PART_PATH;
    private static final String GJENOPPTA_PART_PATH = "/gjenoppta";
    public static final String GJENOPPTA_PATH = BASE_PATH + GJENOPPTA_PART_PATH;
    private static final String HENLEGG_PART_PATH = "/henlegg";
    public static final String HENLEGG_PATH = BASE_PATH + HENLEGG_PART_PATH;
    private static final String OPNE_FOR_ENDRINGER_PART_PATH = "/opne-for-endringer";
    public static final String OPNE_FOR_ENDRINGER_PATH = BASE_PATH + OPNE_FOR_ENDRINGER_PART_PATH;
    private static final String SETT_PA_VENT_PART_PATH = "/sett-pa-vent";
    public static final String SETT_PA_VENT_PATH = BASE_PATH + SETT_PA_VENT_PART_PATH;
    private static final String HANDLING_RETTIGHETER_PART_PATH = "/handling-rettigheter";
    public static final String HANDLING_RETTIGHETER_PATH = BASE_PATH + HANDLING_RETTIGHETER_PART_PATH;
    private static final String ENDRE_VENTEFRIST_PART_PATH = "/endre-pa-vent";
    public static final String ENDRE_VENTEFRIST_PATH = BASE_PATH + ENDRE_VENTEFRIST_PART_PATH;

    private BehandlingsutredningApplikasjonTjeneste behandlingsutredningApplikasjonTjeneste;
    private BehandlingsoppretterApplikasjonTjeneste behandlingsoppretterApplikasjonTjeneste;
    private BehandlingOpprettingTjeneste behandlingOpprettingTjeneste;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;
    private BehandlingDtoTjeneste behandlingDtoTjeneste;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;

    public BehandlingRestTjeneste() {
        // for resteasy
    }

    @Inject
    public BehandlingRestTjeneste(BehandlingsutredningApplikasjonTjeneste behandlingsutredningApplikasjonTjeneste, // NOSONAR
            BehandlingsoppretterApplikasjonTjeneste behandlingsoppretterApplikasjonTjeneste,
            BehandlingOpprettingTjeneste behandlingOpprettingTjeneste,
            BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste,
            FagsakTjeneste fagsakTjeneste,
            HenleggBehandlingTjeneste henleggBehandlingTjeneste,
            BehandlingDtoTjeneste behandlingDtoTjeneste,
            RelatertBehandlingTjeneste relatertBehandlingTjeneste) {
        this.behandlingsutredningApplikasjonTjeneste = behandlingsutredningApplikasjonTjeneste;
        this.behandlingsoppretterApplikasjonTjeneste = behandlingsoppretterApplikasjonTjeneste;
        this.behandlingOpprettingTjeneste = behandlingOpprettingTjeneste;
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.henleggBehandlingTjeneste = henleggBehandlingTjeneste;
        this.behandlingDtoTjeneste = behandlingDtoTjeneste;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
    }

    @POST
    @Path(BEHANDLINGER_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Init hent behandling", tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "202", description = "Hent behandling initiert, Returnerer link til å polle på fremdrift", headers = @Header(name = HttpHeaders.LOCATION)),
            @ApiResponse(responseCode = "303", description = "Behandling tilgjenglig (prosesstasks avsluttet)", headers = @Header(name = HttpHeaders.LOCATION))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Deprecated
    public Response hentBehandling(@NotNull @Valid BehandlingIdDto behandlingIdDto) {
        var behandlingId = behandlingIdDto.getBehandlingId();
        var behandling = behandlingId != null
                ? behandlingsprosessTjeneste.hentBehandling(behandlingId)
                : behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());

        Optional<String> gruppeOpt = behandlingsprosessTjeneste.sjekkOgForberedAsynkInnhentingAvRegisteropplysningerOgKjørProsess(behandling);

        // sender alltid til poll status slik at vi får sjekket på utestående prosess
        // tasks også.
        return Redirect.tilBehandlingPollStatus(behandling.getUuid(), gruppeOpt);
    }

    @GET
    @Path(BEHANDLINGER_STATUS_PART_PATH)
    @Deprecated
    @Operation(description = "Url for å polle på behandling mens behandlingprosessen pågår i bakgrunnen(asynkront)", summary = ("Returnerer link til enten samme (hvis ikke ferdig) eller redirecter til /behandlinger dersom asynkrone operasjoner er ferdig."), tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Status", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AsyncPollingStatus.class))),
            @ApiResponse(responseCode = "303", description = "Behandling tilgjenglig (prosesstasks avsluttet)", headers = @Header(name = HttpHeaders.LOCATION)),
            @ApiResponse(responseCode = "418", description = "ProsessTasks har feilet", headers = @Header(name = HttpHeaders.LOCATION), content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AsyncPollingStatus.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response hentBehandlingMidlertidigStatus(@NotNull @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto,
            @QueryParam("gruppe") @Valid ProsessTaskGruppeIdDto gruppeDto) {
        var behandlingId = behandlingIdDto.getBehandlingId();
        var behandling = behandlingId != null
                ? behandlingsprosessTjeneste.hentBehandling(behandlingId)
                : behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());
        String gruppe = gruppeDto == null ? null : gruppeDto.getGruppe();
        Optional<AsyncPollingStatus> prosessTaskGruppePågår = behandlingsprosessTjeneste.sjekkProsessTaskPågårForBehandling(behandling, gruppe);
        return Redirect.tilBehandlingEllerPollStatus(behandling.getUuid(), prosessTaskGruppePågår.orElse(null));
    }

    @GET
    @Path(BEHANDLINGER_PART_PATH)
    @Deprecated
    @Operation(description = "Hent behandling gitt id", summary = ("Returnerer behandlingen som er tilknyttet id. Dette er resultat etter at asynkrone operasjoner er utført."), tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Behandling", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UtvidetBehandlingDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response hentBehandlingResultat(@NotNull @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) {
        var behandlingId = behandlingIdDto.getBehandlingId();
        var behandling = behandlingId != null
                ? behandlingsprosessTjeneste.hentBehandling(behandlingId)
                : behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());
        AsyncPollingStatus taskStatus = behandlingsprosessTjeneste.sjekkProsessTaskPågårForBehandling(behandling, null).orElse(null);
        UtvidetBehandlingDto dto = behandlingDtoTjeneste.lagUtvidetBehandlingDto(behandling, taskStatus);
        ResponseBuilder responseBuilder = Response.ok().entity(dto);
        return responseBuilder.build();
    }

    @POST
    @Path(SETT_PA_VENT_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Setter behandling på vent", tags = "behandlinger")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public void settBehandlingPaVent(@Parameter(description = "Frist for behandling på vent") @Valid SettBehandlingPaVentDto dto) {
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(dto.getBehandlingId(), dto.getBehandlingVersjon());
        behandlingsutredningApplikasjonTjeneste.settBehandlingPaVent(dto.getBehandlingId(), dto.getFrist(), dto.getVentearsak());
    }

    @POST
    @Path(ENDRE_VENTEFRIST_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Endrer ventefrist for behandling på vent", tags = "behandlinger")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.VENTEFRIST)
    public void endreFristForBehandlingPaVent(
            @Parameter(description = "Frist for behandling på vent") @Valid SettBehandlingPaVentDto dto) {
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(dto.getBehandlingId(), dto.getBehandlingVersjon());
        behandlingsutredningApplikasjonTjeneste.endreBehandlingPaVent(dto.getBehandlingId(), dto.getFrist(), dto.getVentearsak());
    }

    @POST
    @Path(HENLEGG_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henlegger behandling", tags = "behandlinger")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)

    public void henleggBehandling(@Parameter(description = "Henleggelsesårsak") @Valid HenleggBehandlingDto dto) {
        Long behandlingId = dto.getBehandlingId();
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(behandlingId, dto.getBehandlingVersjon());
        BehandlingResultatType årsakKode = tilHenleggBehandlingResultatType(dto.getÅrsakKode());
        henleggBehandlingTjeneste.henleggBehandling(behandlingId, årsakKode, dto.getBegrunnelse());
    }

    @POST
    @Path(GJENOPPTA_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Gjenopptar behandling som er satt på vent", tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "200", description = "Gjenoppta behandling påstartet i bakgrunnen", headers = @Header(name = HttpHeaders.LOCATION))
    })
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)

    public Response gjenopptaBehandling(
            @Parameter(description = "BehandlingId for behandling som skal gjenopptas") @Valid GjenopptaBehandlingDto dto) {
        Long behandlingId = dto.getBehandlingId();
        Long behandlingVersjon = dto.getBehandlingVersjon();

        // precondition - sjekk behandling versjon/lås
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(behandlingId, behandlingVersjon);

        // gjenoppta behandling ( sparkes i gang asynkront, derav redirect til status
        // url under )
        var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
        Optional<String> gruppeOpt = behandlingsprosessTjeneste.gjenopptaBehandling(behandling);
        return Redirect.tilBehandlingPollStatus(behandling.getUuid(), gruppeOpt);
    }

    @POST
    @Path(BYTT_ENHET_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Bytte behandlende enhet", tags = "behandlinger")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)

    public void byttBehandlendeEnhet(@Parameter(description = "Ny enhet som skal byttes") @Valid ByttBehandlendeEnhetDto dto) {
        Long behandlingId = dto.getBehandlingId();
        Long behandlingVersjon = dto.getBehandlingVersjon();
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(behandlingId, behandlingVersjon);

        String enhetId = dto.getEnhetId();
        String enhetNavn = dto.getEnhetNavn();
        String begrunnelse = dto.getBegrunnelse();
        behandlingsutredningApplikasjonTjeneste.byttBehandlendeEnhet(behandlingId, new OrganisasjonsEnhet(enhetId, enhetNavn), begrunnelse,
                HistorikkAktør.SAKSBEHANDLER);
    }

    @PUT
    @Path(BEHANDLINGER_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Opprette ny behandling", tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "202", description = "Opprett ny behandling pågår", headers = @Header(name = HttpHeaders.LOCATION))
    })
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)

    public Response opprettNyBehandling(
            @Parameter(description = "Saksnummer og flagg om det er ny behandling etter klage") @Valid NyBehandlingDto dto) {
        Saksnummer saksnummer = new Saksnummer(Long.toString(dto.getSaksnummer()));
        Optional<Fagsak> funnetFagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true);
        String kode = dto.getBehandlingType().getKode();

        if (funnetFagsak.isEmpty()) {
            throw BehandlingRestTjenesteFeil.FACTORY.fantIkkeFagsak(saksnummer).toException();
        }

        Fagsak fagsak = funnetFagsak.get();

        if (BehandlingType.INNSYN.getKode().equals(kode)) {
            Behandling behandling = behandlingOpprettingTjeneste.opprettBehandling(fagsak, BehandlingType.INNSYN);
            String gruppe = behandlingOpprettingTjeneste.asynkStartBehandlingsprosess(behandling);
            return Redirect.tilBehandlingPollStatus(behandling.getUuid(), Optional.of(gruppe));

        } else if (BehandlingType.ANKE.getKode().equals(kode)) {
            Behandling behandling = behandlingOpprettingTjeneste.opprettBehandlingVedKlageinstans(fagsak, BehandlingType.ANKE);
            String gruppe = behandlingOpprettingTjeneste.asynkStartBehandlingsprosess(behandling);
            return Redirect.tilBehandlingPollStatus(behandling.getUuid(), Optional.of(gruppe));

        } else if (BehandlingType.REVURDERING.getKode().equals(kode)) {
            BehandlingÅrsakType behandlingÅrsakType = BehandlingÅrsakType.fraKode(dto.getBehandlingArsakType().getKode());
            Behandling behandling = behandlingsoppretterApplikasjonTjeneste.opprettRevurdering(fagsak, behandlingÅrsakType);
            String gruppe = behandlingsprosessTjeneste.asynkStartBehandlingsprosess(behandling);
            return Redirect.tilBehandlingPollStatus(behandling.getUuid(), Optional.of(gruppe));

        } else if (BehandlingType.FØRSTEGANGSSØKNAD.getKode().equals(kode)) {
            behandlingsoppretterApplikasjonTjeneste.opprettNyFørstegangsbehandling(fagsak.getId(), saksnummer, dto.getNyBehandlingEtterKlage());
            // ved førstegangssønad opprettes egen task for vurdere denne,
            // sender derfor ikke viderer til prosesser behandling (i motsetning til de
            // andre).
            // må også oppfriske hele sakskomplekset, så sender til fagsak poll url
            return Redirect.tilFagsakPollStatus(fagsak.getSaksnummer(), Optional.empty());

        } else if (BehandlingType.KLAGE.getKode().equals(kode)) {
            Behandling behandling = behandlingOpprettingTjeneste.opprettBehandling(fagsak, BehandlingType.KLAGE);
            String gruppe = behandlingOpprettingTjeneste.asynkStartBehandlingsprosess(behandling);
            return Redirect.tilBehandlingPollStatus(behandling.getUuid(), Optional.of(gruppe));

        } else {
            throw new IllegalArgumentException("Støtter ikke opprette ny behandling for behandlingType:" + kode);
        }

    }

    private static BehandlingResultatType tilHenleggBehandlingResultatType(String årsak) {
        return BehandlingResultatType.getAlleHenleggelseskoder().stream().filter(k -> k.getKode().equals(årsak))
                .findFirst().orElse(null);
    }

    @GET
    @Path(BEHANDLINGER_ALLE_PART_PATH)
    @Operation(description = "Henter alle behandlinger basert på saksnummer", summary = ("Returnerer alle behandlinger som er tilknyttet saksnummer."), tags = "behandlinger")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)

    public List<BehandlingDto> hentBehandlinger(
            @NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer må være et eksisterende saksnummer") @Valid SaksnummerDto s) {
        Saksnummer saksnummer = new Saksnummer(s.getVerdi());
        List<Behandling> behandlinger = behandlingsutredningApplikasjonTjeneste.hentBehandlingerForSaksnummer(saksnummer);
        return behandlingDtoTjeneste.lagBehandlingDtoer(behandlinger);
    }

    @POST
    @Path(OPNE_FOR_ENDRINGER_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Åpner behandling for endringer", tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "200", description = "Åpning av behandling for endringer påstartet i bakgrunnen", headers = @Header(name = HttpHeaders.LOCATION))
    })
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)

    public Response åpneBehandlingForEndringer(
            @Parameter(description = "BehandlingId for behandling som skal åpnes for endringer") @Valid ReåpneBehandlingDto dto) {
        Long behandlingId = dto.getBehandlingId();
        Long behandlingVersjon = dto.getBehandlingVersjon();

        // precondition - sjekk behandling versjon/lås
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(behandlingId, behandlingVersjon);
        var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
        if (behandling.isBehandlingPåVent()) {
            throw BehandlingRestTjenesteFeil.FACTORY.måTaAvVent(behandlingId).toException();
        }
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING)) {
            throw BehandlingRestTjenesteFeil.FACTORY.erBerørtBehandling(behandlingId).toException();
        }
        behandlingsprosessTjeneste.asynkTilbakestillOgÅpneBehandlingForEndringer(behandlingId);
        behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
        return Redirect.tilBehandlingPollStatus(behandling.getUuid(), Optional.empty());
    }

    @GET
    @Path(ANNEN_PART_BEHANDLING_PART_PATH)
    @Operation(description = "Henter annen parts behandling basert på saksnummer", tags = "behandlinger")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)

    public Response hentAnnenPartsGjeldendeBehandling(
            @NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer må være et eksisterende saksnummer") @Valid SaksnummerDto s) {
        Saksnummer saksnummer = new Saksnummer(s.getVerdi());

        Optional<Behandling> behandlingOpt = relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(saksnummer);

        ResponseBuilder responseBuilder;
        if (behandlingOpt.isPresent()) {
            Behandling behandling = behandlingOpt.get();
            AnnenPartBehandlingDto dto = behandlingDtoTjeneste.lagAnnenPartBehandlingDto(behandling);
            responseBuilder = Response.ok().entity(dto);
        } else {
            responseBuilder = Response.ok();
        }

        return responseBuilder.build();
    }

    @GET
    @Path(HANDLING_RETTIGHETER_PART_PATH)
    @Operation(description = "Henter rettigheter for lovlige behandlingsoperasjoner", tags = "behandlinger")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)

    public BehandlingRettigheterDto hentBehandlingOperasjonRettigheter(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Behandling behandling = behandlingsprosessTjeneste.hentBehandling(uuidDto.getBehandlingUuid());
        Boolean harSoknad = behandlingDtoTjeneste.finnBehandlingOperasjonRettigheter(behandling);
        // TODO (TOR) Denne skal etterkvart returnere rettighetene knytta til
        // behandlingsmeny i frontend
        return new BehandlingRettigheterDto(harSoknad);
    }

    private interface BehandlingRestTjenesteFeil extends DeklarerteFeil {
        BehandlingRestTjenesteFeil FACTORY = FeilFactory.create(BehandlingRestTjenesteFeil.class); // NOSONAR

        @TekniskFeil(feilkode = "FP-760410", feilmelding = "Fant ikke fagsak med saksnummer %s", logLevel = ERROR, exceptionClass = TomtResultatException.class)
        Feil fantIkkeFagsak(Saksnummer saksnummer);

        @FunksjonellFeil(feilkode = "FP-722320", feilmelding = "Behandling må tas av vent før den kan åpnes", løsningsforslag = "Ta behandling av vent")
        Feil måTaAvVent(Long behandlingId);

        @FunksjonellFeil(feilkode = "FP-722321", feilmelding = "Behandling er berørt må gjennomføres", løsningsforslag = "Behandle ferdig berørt og opprett revurdering")
        Feil erBerørtBehandling(Long behandlingId);
    }
}
