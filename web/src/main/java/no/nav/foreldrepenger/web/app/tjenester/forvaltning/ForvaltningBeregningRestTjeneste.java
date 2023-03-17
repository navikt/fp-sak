package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.revurdering.satsregulering.AutomatiskGrunnbelopReguleringTask;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.BeregningSatsDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltningBeregning")
@ApplicationScoped
@Transactional
public class ForvaltningBeregningRestTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningBeregningRestTjeneste.class);

    private FagsakRepository fagsakRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;
    private BeregningsresultatRepository beregningsresultatRepository;

    @Inject
    public ForvaltningBeregningRestTjeneste(ProsessTaskTjeneste taskTjeneste,
                                            BehandlingRepository behandlingRepository,
                                            BeregningsresultatRepository beregningsresultatRepository,
                                            FagsakRepository fagsakRepository,
                                            BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider) {
        this.taskTjeneste = taskTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.fagsakRepository = fagsakRepository;
        this.beregningsgrunnlagInputProvider = beregningsgrunnlagInputProvider;
    }

    public ForvaltningBeregningRestTjeneste() {
        // CDI
    }

    @GET
    @Path("/satsHentGjeldende")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent liste av gjeldende eller nyeste sats", tags = "FORVALTNING-beregning", responses = {
            @ApiResponse(responseCode = "200", description = "Gjeldende satser", content = @Content(array = @ArraySchema(arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = BeregningSatsDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public List<BeregningSatsDto> hentGjeldendeSatser() {
        return Set.of(BeregningSatsType.ENGANG, BeregningSatsType.GRUNNBELØP, BeregningSatsType.GSNITT).stream()
                .map(beregningsresultatRepository::finnGjeldendeSats)
                .map(BeregningSatsDto::new)
                .toList();
    }

    @POST
    @Path("/satsLagreNy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagre ny sats", tags = "FORVALTNING-beregning", responses = {
            @ApiResponse(responseCode = "200", description = "Gjeldende satser", content = @Content(array = @ArraySchema(arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = BeregningSatsDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public List<BeregningSatsDto> lagreNySats(@BeanParam @Valid @NotNull BeregningSatsDto dto) {
        var type = dto.getSatsType();
        var brukTom = dto.getSatsTom() != null ? dto.getSatsTom() : LocalDate.now().plusYears(99);
        var gjeldende = beregningsresultatRepository.finnGjeldendeSats(type);
        if (!sjekkVerdierOK(dto, gjeldende, brukTom))
            throw new ForvaltningException("Ulovlige verdier " + dto);
        LOG.warn("SATSJUSTERTING: sjekk med produkteier om det er ventet, noter usedId i loggen {}", dto);
        gjeldende.setTomDato(dto.getSatsFom().minusDays(1));
        beregningsresultatRepository.lagreSats(gjeldende);
        var nysats = new BeregningSats(type, DatoIntervallEntitet.fraOgMedTilOgMed(dto.getSatsFom(), brukTom), dto.getSatsVerdi());
        beregningsresultatRepository.lagreSats(nysats);
        var nygjeldende = beregningsresultatRepository.finnGjeldendeSats(type);
        return Set.of(gjeldende, nygjeldende).stream().map(BeregningSatsDto::new).toList();
    }

    private boolean sjekkVerdierOK(BeregningSatsDto dto, BeregningSats gjeldende, LocalDate brukTom) {
        if (!brukTom.isAfter(dto.getSatsFom()) || !dto.getSatsFom().isAfter(gjeldende.getPeriode().getFomDato()))
            return false;
        if (BeregningSatsType.GRUNNBELØP.equals(gjeldende.getSatsType())) {
            return gjeldende.getPeriode().getTomDato().isAfter(dto.getSatsFom()) && Month.MAY.equals(dto.getSatsFom().getMonth())
                    && dto.getSatsFom().getDayOfMonth() == 1;
        }
        if (BeregningSatsType.ENGANG.equals(gjeldende.getSatsType())) {
            return gjeldende.getPeriode().getTomDato().isAfter(dto.getSatsFom());
        }
        // GSNITT skal være bounded
        return dto.getSatsTom() != null && dto.getSatsFom().equals(gjeldende.getPeriode().getTomDato().plusDays(1))
                && dto.getSatsTom().equals(dto.getSatsFom().plusYears(1).minusDays(1));
    }

    @POST
    @Path("/opprettGreguleringEnkeltSak")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Steng fagsak og flytt til Infotrygd", tags = "FORVALTNING-fagsak", responses = {
        @ApiResponse(responseCode = "200", description = "Flyttet fagsak.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK)
    public Response opprettGreguleringEnkeltSak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                                    @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow();
        var åpneBehandlinger = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsak.getId())
            .stream().anyMatch(SpesialBehandling::erIkkeSpesialBehandling);
        if (no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType()) || åpneBehandlinger) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskGrunnbelopReguleringTask.class);
        prosessTaskData.setFagsak(fagsak.getId(), fagsak.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(50);
        prosessTaskData.setProperty(AutomatiskGrunnbelopReguleringTask.MANUELL_KEY, "true");
        taskTjeneste.lagre(prosessTaskData);
        return Response.ok().build();
    }

    private Behandling getBehandling(ForvaltningBehandlingIdDto dto) {
        return behandlingRepository.hentBehandling(dto.getBehandlingUuid());
    }

    @POST
    @Path("/hentRefusjonskravperioderInput")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter input for beregning", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response hentRefusjonskravperioderInput(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandling = getBehandling(dto);
        var inputTjeneste = beregningsgrunnlagInputProvider.getTjeneste(behandling.getFagsakYtelseType());
        var beregningsgrunnlagInput = inputTjeneste.lagInput(behandling.getId());
        if (beregningsgrunnlagInput == null) {
            return Response.noContent().build();
        }
        var json = StandardJsonConfig.toJson(beregningsgrunnlagInput.getKravPrArbeidsgiver());
        return Response.ok(json).build();
    }

    @POST
    @Path("/hentBeregningsgrunnlagInput")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter input for beregning", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response hentBeregningsgrunnlagInput(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandling = getBehandling(dto);
        var inputTjeneste = beregningsgrunnlagInputProvider.getTjeneste(behandling.getFagsakYtelseType());
        var beregningsgrunnlagInput = inputTjeneste.lagInput(behandling.getId());
        var kalkulatorInputDto = MapTilKalkulatorInput.map(beregningsgrunnlagInput);
        if (kalkulatorInputDto == null) {
            return Response.noContent().build();
        }
        return Response.ok(kalkulatorInputDto).build();
    }

}
