package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.satsregulering.GrunnbeløpReguleringTask;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.SatsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.mappers.KalkulusInputTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.BeregningSatsDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.EndreInntektsmeldingDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@Path("/forvaltningBeregning")
@ApplicationScoped
@Transactional
public class ForvaltningBeregningRestTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningBeregningRestTjeneste.class);

    private FagsakRepository fagsakRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private SatsRepository satsRepository;
    private KalkulusInputTjeneste kalkulusInputTjeneste;

    @Inject
    public ForvaltningBeregningRestTjeneste(ProsessTaskTjeneste taskTjeneste,
                                            BehandlingRepository behandlingRepository,
                                            FagsakRepository fagsakRepository,
                                            InntektArbeidYtelseTjeneste iayTjeneste,
                                            SatsRepository satsRepository,
                                            KalkulusInputTjeneste kalkulusInputTjeneste) {
        this.taskTjeneste = taskTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.iayTjeneste = iayTjeneste;
        this.satsRepository = satsRepository;
        this.kalkulusInputTjeneste = kalkulusInputTjeneste;
    }

    public ForvaltningBeregningRestTjeneste() {
        // CDI
    }

    @GET
    @Path("/satsHentGjeldende")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent liste av gjeldende eller nyeste sats", tags = "FORVALTNING-beregning", responses = {@ApiResponse(responseCode = "200", description = "Gjeldende satser", content = @Content(array = @ArraySchema(arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = BeregningSatsDto.class)), mediaType = MediaType.APPLICATION_JSON))})
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public List<BeregningSatsDto> hentGjeldendeSatser() {
        return Set.of(BeregningSatsType.ENGANG, BeregningSatsType.GRUNNBELØP, BeregningSatsType.GSNITT)
            .stream()
            .map(satsRepository::finnGjeldendeSats)
            .map(BeregningSatsDto::new)
            .toList();
    }

    @POST
    @Path("/satsLagreNy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagre ny sats", tags = "FORVALTNING-beregning", responses = {@ApiResponse(responseCode = "200", description = "Gjeldende satser", content = @Content(array = @ArraySchema(arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = BeregningSatsDto.class)), mediaType = MediaType.APPLICATION_JSON))})
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public List<BeregningSatsDto> lagreNySats(@BeanParam @Valid @NotNull BeregningSatsDto dto) {
        var type = dto.getSatsType();
        var brukTom = dto.getSatsTom() != null ? dto.getSatsTom() : LocalDate.now().plusYears(99);
        var gjeldende = satsRepository.finnGjeldendeSats(type);
        var eksisterende = new BeregningSatsDto(gjeldende);

        if (Objects.equals(gjeldende.getPeriode().getFomDato(), dto.getSatsFom())) {
            // Ved behov for å oppdatere gjeldende sats verdi eller tom
            if (!Objects.equals(gjeldende.getVerdi(), dto.getSatsVerdi()) && brukTom.isAfter(gjeldende.getPeriode().getFomDato())) {
                LOG.warn("SATSJUSTERTING oppdatering: sjekk med produkteier om det er ventet, noter usedId i loggen {}", dto);
                gjeldende.setVerdi(dto.getSatsVerdi());
                gjeldende.setTomDato(brukTom);
                satsRepository.lagreSats(gjeldende);
            } else {
                throw new ForvaltningException("Ulovlige verdier " + dto);
            }
        } else {
            // Nytt innslag. Sett sluttdato på gjeldende og legg til ny
            if (!sjekkVerdierOK(dto, gjeldende, brukTom))
                throw new ForvaltningException("Ulovlige verdier " + dto);
            LOG.warn("SATSJUSTERTING: sjekk med produkteier om det er ventet, noter usedId i loggen {}", dto);
            gjeldende.setTomDato(dto.getSatsFom().minusDays(1));
            satsRepository.lagreSats(gjeldende);
            var nysats = new BeregningSats(type, DatoIntervallEntitet.fraOgMedTilOgMed(dto.getSatsFom(), brukTom), dto.getSatsVerdi());
            satsRepository.lagreSats(nysats);
        }
        var nygjeldende = satsRepository.finnGjeldendeSats(type);
        return List.of(eksisterende, new BeregningSatsDto(nygjeldende));
    }

    private boolean sjekkVerdierOK(BeregningSatsDto dto, BeregningSats gjeldende, LocalDate brukTom) {
        if (!brukTom.isAfter(dto.getSatsFom()) || !dto.getSatsFom().isAfter(gjeldende.getPeriode().getFomDato()))
            return false;
        if (BeregningSatsType.GRUNNBELØP.equals(gjeldende.getSatsType())) {
            return gjeldende.getPeriode().getTomDato().isAfter(dto.getSatsFom()) && Month.MAY.equals(dto.getSatsFom().getMonth()) && dto.getSatsFom().getDayOfMonth() == 1;
        }
        if (BeregningSatsType.ENGANG.equals(gjeldende.getSatsType())) {
            return gjeldende.getPeriode().getTomDato().isAfter(dto.getSatsFom());
        }
        // GSNITT skal være bounded
        return dto.getSatsTom() != null && dto.getSatsFom().equals(gjeldende.getPeriode().getTomDato().plusDays(1)) && dto.getSatsTom()
            .equals(dto.getSatsFom().plusYears(1).minusDays(1));
    }

    @POST
    @Path("/opprettGreguleringEnkeltSak")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Steng fagsak og flytt til Infotrygd", tags = "FORVALTNING-fagsak", responses = {@ApiResponse(responseCode = "200", description = "Flyttet fagsak.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))), @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."), @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")})
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response opprettGreguleringEnkeltSak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class) @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow();
        var åpneBehandlinger = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsak.getId())
            .stream().anyMatch(SpesialBehandling::erIkkeSpesialBehandling);
        if (no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType()) || åpneBehandlinger) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var prosessTaskData = ProsessTaskData.forProsessTask(GrunnbeløpReguleringTask.class);
        prosessTaskData.setFagsak(fagsak.getSaksnummer().getVerdi(), fagsak.getId());
        prosessTaskData.setProperty(GrunnbeløpReguleringTask.MANUELL_KEY, "true");
        taskTjeneste.lagre(prosessTaskData);
        return Response.ok().build();
    }

    private Behandling getBehandling(ForvaltningBehandlingIdDto dto) {
        return behandlingRepository.hentBehandling(dto.getBehandlingUuid());
    }

    @POST
    @Path("/stoppRefusjon")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Endrer refusjon på inntektsmelding for en gitt journalpost", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response opphørRefusjonInntektsmelding(@NotNull @Valid EndreInntektsmeldingDto dto) {
        var behandling = behandlingRepository.hentBehandling(dto.getBehandlingUuid());
        var inntektsmeldinger = iayTjeneste.finnGrunnlag(behandling.getId())
            .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger)
            .map(InntektsmeldingAggregat::getAlleInntektsmeldinger)
            .orElse(Collections.emptyList());
        var matchetInntektsmelding = inntektsmeldinger.stream().filter(im -> im.getJournalpostId().getVerdi().equals(dto.getJournalpostId())).findFirst();
        if (matchetInntektsmelding.isEmpty()) {
            var msg = String.format("Finner ikke inntektsmelding med journalpostId %s på behandling med uuid %s ", dto.getJournalpostId(),
                dto.getBehandlingUuid());
            return Response.ok(msg).build();
        }
        if (dto.getRefusjonsendringer().isEmpty() && dto.getRefusjonOpphørFom() == null) {
            return Response.ok("Det er ikke oppgitt hverken opphørsdato for refusjojn eller en liste med refusjonsendringer, ingenting å endre.").build();
        }
        var refusjonsendringer = dto.getRefusjonsendringer()
            .stream()
            .map(r -> new OverstyrInntektsmeldingTask.InntektsmeldingEndring.Refusjonsendring(r.getFom(), r.getBeløp()))
            .toList();
        var taskParam = new OverstyrInntektsmeldingTask.InntektsmeldingEndring(dto.getJournalpostId(), behandling.getId(), dto.getRefusjonOpphørFom(), dto.getRefusjonPrMndFraStart(),
            KontekstHolder.getKontekst().getUid(), refusjonsendringer, dto.getStartdatoPermisjon());
        var task = ProsessTaskData.forProsessTask(OverstyrInntektsmeldingTask.class);
        task.setPayload(DefaultJsonMapper.toJson(taskParam));
        task.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
        taskTjeneste.lagre(task);
        return Response.ok().build();
    }

    @POST
    @Path("/hentBeregningsgrunnlagInput")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter input for beregning som sendes til kalkulus", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response hentBeregningsgrunnlagInput(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandling = getBehandling(dto);
        var kalkulatorInputDto = kalkulusInputTjeneste.lagKalkulusInput(BehandlingReferanse.fra(behandling));
        if (kalkulatorInputDto == null) {
            return Response.noContent().build();
        }
        return Response.ok(kalkulatorInputDto).build();
    }
}
