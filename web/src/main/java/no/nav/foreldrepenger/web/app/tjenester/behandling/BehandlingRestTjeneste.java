package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.web.app.exceptions.FeilDto;
import no.nav.foreldrepenger.web.app.exceptions.FeilType;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsoppretterTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingOperasjonerDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.ByttBehandlendeEnhetDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.DtoMedBehandlingId;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.GjenopptaBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.HenleggBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.NyBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.Redirect;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.ReåpneBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.SettBehandlingPaVentDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.AnnenPartBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.ProsessTaskGruppeIdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.UtvidetBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.verge.VergeTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@ApplicationScoped
@Transactional
@Path(BehandlingRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class BehandlingRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingRestTjeneste.class);

    static final String BASE_PATH = "/behandlinger";
    private static final String ANNEN_PART_BEHANDLING_PART_PATH = "/annen-part-behandling";
    public static final String ANNEN_PART_BEHANDLING_PATH = BASE_PATH + ANNEN_PART_BEHANDLING_PART_PATH; // NOSONAR TFP-2234
    private static final String BEHANDLINGER_ALLE_PART_PATH = "/alle";
    public static final String BEHANDLINGER_ALLE_PATH = BASE_PATH + BEHANDLINGER_ALLE_PART_PATH; // NOSONAR TFP-2234
    private static final String BEHANDLINGER_PART_PATH = "";
    private static final String BEHANDLINGER_STATUS_PART_PATH = "/status";
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
    private static final String RETTIGHETER_PART_PATH = "/rettigheter";
    public static final String RETTIGHETER_PATH = BASE_PATH + RETTIGHETER_PART_PATH;
    private static final String ENDRE_VENTEFRIST_PART_PATH = "/endre-pa-vent";
    public static final String ENDRE_VENTEFRIST_PATH = BASE_PATH + ENDRE_VENTEFRIST_PART_PATH;

    private BehandlingsutredningTjeneste behandlingsutredningTjeneste;
    private BehandlingsoppretterTjeneste behandlingsoppretterTjeneste;
    private BehandlingOpprettingTjeneste behandlingOpprettingTjeneste;
    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;
    private BehandlingDtoTjeneste behandlingDtoTjeneste;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private VergeTjeneste vergeTjeneste;
    private TotrinnTjeneste totrinnTjeneste;

    public BehandlingRestTjeneste() {
        // for resteasy
    }

    @Inject
    public BehandlingRestTjeneste(BehandlingsutredningTjeneste behandlingsutredningTjeneste, // NOSONAR
                                  BehandlingsoppretterTjeneste behandlingsoppretterTjeneste,
                                  BehandlingOpprettingTjeneste behandlingOpprettingTjeneste,
                                  BehandlingsprosessTjeneste behandlingsprosessTjeneste,
                                  FagsakTjeneste fagsakTjeneste,
                                  VergeTjeneste vergeTjeneste,
                                  HenleggBehandlingTjeneste henleggBehandlingTjeneste,
                                  BehandlingDtoTjeneste behandlingDtoTjeneste,
                                  RelatertBehandlingTjeneste relatertBehandlingTjeneste,
                                  TotrinnTjeneste totrinnTjeneste) {
        this.behandlingsutredningTjeneste = behandlingsutredningTjeneste;
        this.behandlingsoppretterTjeneste = behandlingsoppretterTjeneste;
        this.behandlingOpprettingTjeneste = behandlingOpprettingTjeneste;
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.vergeTjeneste = vergeTjeneste;
        this.henleggBehandlingTjeneste = henleggBehandlingTjeneste;
        this.behandlingDtoTjeneste = behandlingDtoTjeneste;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
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
    public Response hentBehandling(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class)
        @NotNull @Valid BehandlingIdDto behandlingIdDto) {
        if (behandlingIdDto.getBehandlingId() != null) LOG.info("Behandlingrest hentBeh kall med behandlingId");
        var behandling = getBehandling(behandlingIdDto);

        var gruppeOpt = behandlingsprosessTjeneste.sjekkOgForberedAsynkInnhentingAvRegisteropplysningerOgKjørProsess(behandling);

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
    public Response hentBehandlingMidlertidigStatus(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class)
        @NotNull @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto,
            @TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.TaskgruppeAbacDataSupplier.class) @QueryParam("gruppe") @Valid ProsessTaskGruppeIdDto gruppeDto) {
        if (behandlingIdDto.getBehandlingId() != null) LOG.info("Behandlingrest hentBehMidlStatus kall med behandlingId");
        var behandling = getBehandling(behandlingIdDto);
        var gruppe = gruppeDto == null ? null : gruppeDto.getGruppe();
        var prosessTaskGruppePågår = behandlingsprosessTjeneste.sjekkProsessTaskPågårForBehandling(behandling, gruppe);
        return Redirect.tilBehandlingEllerPollStatus(behandling.getUuid(), prosessTaskGruppePågår.orElse(null));
    }

    private Behandling getBehandling(BehandlingIdDto behandlingIdDto) {
        return behandlingIdDto.getBehandlingId() != null
                ? behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingId())
                : behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());
    }

    @GET
    @Path(BEHANDLINGER_PART_PATH)
    @Deprecated
    @Operation(description = "Hent behandling gitt id", summary = ("Returnerer behandlingen som er tilknyttet id. Dette er resultat etter at asynkrone operasjoner er utført."), tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Behandling", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UtvidetBehandlingDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response hentBehandlingResultat(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class)
        @NotNull @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) {
        if (behandlingIdDto.getBehandlingId() != null) LOG.info("Behandlingrest hentBehRes kall med behandlingId");
        var behandling = getBehandling(behandlingIdDto);
        var taskStatus = behandlingsprosessTjeneste.sjekkProsessTaskPågårForBehandling(behandling, null).orElse(null);
        var dto = behandlingDtoTjeneste.lagUtvidetBehandlingDto(behandling, taskStatus);
        var responseBuilder = Response.ok().entity(dto);
        return responseBuilder.build();
    }

    @POST
    @Path(SETT_PA_VENT_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Setter behandling på vent", tags = "behandlinger")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public void settBehandlingPaVent(@TilpassetAbacAttributt(supplierClass = LocalBehandlingIdAbacDataSupplier.class)
        @Parameter(description = "Frist for behandling på vent") @Valid SettBehandlingPaVentDto dto) {
        var behandling = getBehandling(dto);

        behandlingsutredningTjeneste.kanEndreBehandling(behandling, dto.getBehandlingVersjon());
        behandlingsutredningTjeneste.settBehandlingPaVent(behandling.getId(), dto.getFrist(), dto.getVentearsak());
    }

    @POST
    @Path(ENDRE_VENTEFRIST_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Endrer ventefrist for behandling på vent", tags = "behandlinger")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.VENTEFRIST)
    public void endreFristForBehandlingPaVent(@TilpassetAbacAttributt(supplierClass = LocalBehandlingIdAbacDataSupplier.class)
            @Parameter(description = "Frist for behandling på vent") @Valid SettBehandlingPaVentDto dto) {
        var behandling = getBehandling(dto);
        behandlingsutredningTjeneste.kanEndreBehandling(behandling, dto.getBehandlingVersjon());
        behandlingsutredningTjeneste.endreBehandlingPaVent(behandling.getId(), dto.getFrist(), dto.getVentearsak());
    }

    @POST
    @Path(HENLEGG_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henlegger behandling", tags = "behandlinger")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)

    public void henleggBehandling(@TilpassetAbacAttributt(supplierClass = LocalBehandlingIdAbacDataSupplier.class)
        @Parameter(description = "Henleggelsesårsak") @Valid HenleggBehandlingDto dto) {
        var behandling = getBehandling(dto);
        behandlingsutredningTjeneste.kanEndreBehandling(behandling, dto.getBehandlingVersjon());
        var årsakKode = tilHenleggBehandlingResultatType(dto.getÅrsakKode());
        henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), årsakKode, dto.getBegrunnelse());
    }

    private Behandling getBehandling(DtoMedBehandlingId dto) {
        return behandlingsprosessTjeneste.hentBehandling(dto.getBehandlingUuid());
    }

    @POST
    @Path(GJENOPPTA_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Gjenopptar behandling som er satt på vent", tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "200", description = "Gjenoppta behandling påstartet i bakgrunnen", headers = @Header(name = HttpHeaders.LOCATION))
    })
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response gjenopptaBehandling(@TilpassetAbacAttributt(supplierClass = LocalBehandlingIdAbacDataSupplier.class)
            @Parameter(description = "BehandlingId for behandling som skal gjenopptas") @Valid GjenopptaBehandlingDto dto) {
        var behandlingVersjon = dto.getBehandlingVersjon();
        var behandling = getBehandling(dto);

        // precondition - sjekk behandling versjon/lås
        behandlingsutredningTjeneste.kanEndreBehandling(behandling, behandlingVersjon);

        // gjenoppta behandling ( sparkes i gang asynkront, derav redirect til status
        // url under )
        var gruppeOpt = behandlingsprosessTjeneste.gjenopptaBehandling(behandling);
        return Redirect.tilBehandlingPollStatus(behandling.getUuid(), gruppeOpt);
    }

    @POST
    @Path(BYTT_ENHET_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Bytte behandlende enhet", tags = "behandlinger")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public void byttBehandlendeEnhet(@TilpassetAbacAttributt(supplierClass = LocalBehandlingIdAbacDataSupplier.class)
        @Parameter(description = "Ny enhet som skal byttes") @Valid ByttBehandlendeEnhetDto dto) {
        var behandlingVersjon = dto.getBehandlingVersjon();
        var behandling = getBehandling(dto);
        behandlingsutredningTjeneste.kanEndreBehandling(behandling, behandlingVersjon);

        var enhetId = dto.getEnhetId();
        var enhetNavn = dto.getEnhetNavn();
        var begrunnelse = dto.getBegrunnelse();
        behandlingsutredningTjeneste.byttBehandlendeEnhet(behandling.getId(), new OrganisasjonsEnhet(enhetId, enhetNavn), begrunnelse,
                HistorikkAktør.SAKSBEHANDLER);
    }

    @PUT
    @Path(BEHANDLINGER_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Opprette ny behandling", tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "202", description = "Opprett ny behandling pågår", headers = @Header(name = HttpHeaders.LOCATION))
    })
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response opprettNyBehandling(@TilpassetAbacAttributt(supplierClass = NyBehandlingAbacDataSupplier.class)
            @Parameter(description = "Saksnummer og flagg om det er ny behandling etter klage") @Valid NyBehandlingDto dto) {
        var saksnummer = new Saksnummer(Long.toString(dto.getSaksnummer()));
        var funnetFagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true);
        var kode = BehandlingType.fraKode(dto.getBehandlingType().getKode());

        if (funnetFagsak.isEmpty()) {
            return notFound(saksnummer);
        }

        var fagsak = funnetFagsak.get();
        try {
            if (!behandlingsoppretterTjeneste.kanOppretteNyBehandlingAvType(fagsak.getId(), kode)) {
                LOG.info("BEHREST opprett behandling får nei for sak {} behandlingtype {}", fagsak.getSaksnummer().getVerdi(), kode.getKode());
            }
        } catch (Exception e) {
            LOG.info("BEHREST opprett behandling feil", e);
        }

        if (BehandlingType.INNSYN.equals(kode)) {
            var behandling = behandlingOpprettingTjeneste.opprettBehandling(fagsak, BehandlingType.INNSYN);
            var gruppe = behandlingOpprettingTjeneste.asynkStartBehandlingsprosess(behandling);
            return Redirect.tilBehandlingPollStatus(behandling.getUuid(), Optional.of(gruppe));

        }
        if (BehandlingType.ANKE.equals(kode)) {
            var behandling = behandlingOpprettingTjeneste.opprettBehandlingVedKlageinstans(fagsak, BehandlingType.ANKE);
            var gruppe = behandlingOpprettingTjeneste.asynkStartBehandlingsprosess(behandling);
            return Redirect.tilBehandlingPollStatus(behandling.getUuid(), Optional.of(gruppe));

        }
        if (BehandlingType.REVURDERING.equals(kode)) {
            var behandlingÅrsakType = BehandlingÅrsakType.fraKode(dto.getBehandlingArsakType().getKode());
            var behandling = behandlingsoppretterTjeneste.opprettRevurdering(fagsak, behandlingÅrsakType);
            var gruppe = behandlingsprosessTjeneste.asynkStartBehandlingsprosess(behandling);
            return Redirect.tilBehandlingPollStatus(behandling.getUuid(), Optional.of(gruppe));

        }
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(kode)) {
            behandlingsoppretterTjeneste.opprettNyFørstegangsbehandling(fagsak.getId(), saksnummer, dto.getNyBehandlingEtterKlage());
            // ved førstegangssønad opprettes egen task for vurdere denne,
            // sender derfor ikke viderer til prosesser behandling (i motsetning til de
            // andre).
            // må også oppfriske hele sakskomplekset, så sender til fagsak poll url
            return Redirect.tilFagsakPollStatus(fagsak.getSaksnummer(), Optional.empty());

        }
        if (BehandlingType.KLAGE.equals(kode)) {
            var behandling = behandlingOpprettingTjeneste.opprettBehandling(fagsak, BehandlingType.KLAGE);
            var gruppe = behandlingOpprettingTjeneste.asynkStartBehandlingsprosess(behandling);
            return Redirect.tilBehandlingPollStatus(behandling.getUuid(), Optional.of(gruppe));

        }
        throw new IllegalArgumentException("Støtter ikke opprette ny behandling for behandlingType:" + kode);

    }

    private Response notFound(Saksnummer saksnummer) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new FeilDto(FeilType.TOMT_RESULTAT_FEIL, "Fant ikke fagsak med saksnummer " + saksnummer))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private static BehandlingResultatType tilHenleggBehandlingResultatType(String årsak) {
        return BehandlingResultatType.getAlleHenleggelseskoder().stream().filter(k -> k.getKode().equals(årsak))
                .findFirst().orElse(null);
    }

    @GET
    @Path(BEHANDLINGER_ALLE_PART_PATH)
    @Operation(description = "Henter alle behandlinger basert på saksnummer", summary = ("Returnerer alle behandlinger som er tilknyttet saksnummer."), tags = "behandlinger")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)

    public List<BehandlingDto> hentBehandlinger(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
            @NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer må være et eksisterende saksnummer") @Valid SaksnummerDto s) {
        var saksnummer = new Saksnummer(s.getVerdi());
        var behandlinger = behandlingsutredningTjeneste.hentBehandlingerForSaksnummer(saksnummer);
        return behandlingDtoTjeneste.lagBehandlingDtoer(behandlinger);
    }

    @POST
    @Path(OPNE_FOR_ENDRINGER_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Åpner behandling for endringer", tags = "behandlinger", responses = {
            @ApiResponse(responseCode = "200", description = "Åpning av behandling for endringer påstartet i bakgrunnen", headers = @Header(name = HttpHeaders.LOCATION))
    })
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)

    public Response åpneBehandlingForEndringer(@TilpassetAbacAttributt(supplierClass = LocalBehandlingIdAbacDataSupplier.class)
            @Parameter(description = "BehandlingId for behandling som skal åpnes for endringer") @Valid ReåpneBehandlingDto dto) {
        var behandlingVersjon = dto.getBehandlingVersjon();

        var behandling = getBehandling(dto);
        var behandlingId = behandling.getId();

        // precondition - sjekk behandling versjon/lås
        behandlingsutredningTjeneste.kanEndreBehandling(behandling, behandlingVersjon);
        if (behandling.isBehandlingPåVent()) {
            throw new FunksjonellException("FP-722320", "Behandling må tas av vent før den kan åpnes",
                "Ta behandling av vent");
        }
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING)) {
            throw new FunksjonellException("FP-722321", "Behandling er berørt må gjennomføres. BehandlingId=" + behandlingId,
                "Behandle ferdig berørt og opprett revurdering");
        }
        behandlingsprosessTjeneste.asynkTilbakestillOgÅpneBehandlingForEndringer(behandling);
        behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
        return Redirect.tilBehandlingPollStatus(behandling.getUuid(), Optional.empty());
    }

    @GET
    @Path(ANNEN_PART_BEHANDLING_PART_PATH)
    @Operation(description = "Henter annen parts behandling basert på saksnummer", tags = "behandlinger")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)

    public Response hentAnnenPartsGjeldendeBehandling(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
            @NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer må være et eksisterende saksnummer") @Valid SaksnummerDto s) {
        var saksnummer = new Saksnummer(s.getVerdi());

        return relatertBehandlingTjeneste.hentAnnenPartsGjeldendeYtelsesBehandling(saksnummer)
            .map(behandling -> new AnnenPartBehandlingDto(new SaksnummerDto(behandling.getFagsak().getSaksnummer()), behandling.getUuid()))
            .map(apDto -> Response.ok().entity(apDto).build()).orElseGet(() -> Response.ok().build());
    }

    @GET
    @Path(RETTIGHETER_PART_PATH)
    @Operation(description = "Henter rettigheter for menyvalg", tags = "behandlinger")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public BehandlingOperasjonerDto hentMenyOpsjoner(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {

        var behandling = behandlingsprosessTjeneste.hentBehandling(uuidDto.getBehandlingUuid());

        return lovligeOperasjoner(behandling);
    }

    public static class LocalBehandlingIdAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (DtoMedBehandlingId) obj;
            return AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.getBehandlingUuid());
        }
    }

    public static class NyBehandlingAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (NyBehandlingDto) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, req.getSaksnummer());
        }
    }

    private BehandlingOperasjonerDto lovligeOperasjoner(Behandling b) {
        if (b.erSaksbehandlingAvsluttet()) {
            return BehandlingOperasjonerDto.builder(b.getUuid()).build(); // Skal ikke foreta menyvalg lenger
        }
        if (BehandlingStatus.FATTER_VEDTAK.equals(b.getStatus())) {
            var tilgokjenning = b.getAnsvarligSaksbehandler() != null && !b.getAnsvarligSaksbehandler().equalsIgnoreCase(SubjectHandler.getSubjectHandler().getUid());
            return BehandlingOperasjonerDto.builder(b.getUuid()).medTilGodkjenning(tilgokjenning).build();
        }
        var kanÅpnesForEndring = b.erRevurdering() && !b.isBehandlingPåVent() &&
            !b.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING) && !b.erKøet() &&
            !FagsakYtelseType.ENGANGSTØNAD.equals(b.getFagsakYtelseType());
        var totrinnRetur = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(b).stream()
            .anyMatch(tt -> !tt.isGodkjent());
        return BehandlingOperasjonerDto.builder(b.getUuid())
            .medTilGodkjenning(false)
            .medFraBeslutter(!b.isBehandlingPåVent() && totrinnRetur)
            .medKanBytteEnhet(!b.erKøet())
            .medKanHenlegges(true)
            .medKanSettesPaVent(!b.isBehandlingPåVent())
            .medKanGjenopptas(b.isBehandlingPåVent() && !b.erKøet())
            .medKanOpnesForEndringer(kanÅpnesForEndring)
            .medKanSendeMelding(!b.isBehandlingPåVent())
            .medVergemeny(vergeTjeneste.utledBehandlingsmeny(b.getId()).getVergeBehandlingsmeny())
            .build();
    }
}
