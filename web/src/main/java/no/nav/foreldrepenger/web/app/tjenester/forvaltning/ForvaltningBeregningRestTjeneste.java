package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.folketrygdloven.kalkulator.felles.BeregningUtils;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektArbeidYtelseGrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseAnvistDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseFilterDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.AktørId;
import no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.FagsakYtelseType;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.task.OpprettGrunnbeløpTask;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.task.TilbakerullingBeregningTask;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.IAYMapperTilKalkulus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.BeregningSatsDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.SaksnummerEnhetDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("/forvaltningBeregning")
@ApplicationScoped
@Transactional
public class ForvaltningBeregningRestTjeneste {
    private static final Period MELDEKORT_PERIODE_UTV = Period.parse("P30D");
    private static final Logger LOGGER = LoggerFactory.getLogger(ForvaltningBeregningRestTjeneste.class);

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;
    private BeregningsresultatRepository beregningsresultatRepository;

    @Inject
    public ForvaltningBeregningRestTjeneste(
            BeregningsgrunnlagRepository beregningsgrunnlagRepository, BehandlingRepositoryProvider repositoryProvider,
            ProsessTaskRepository prosessTaskRepository, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
            BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.prosessTaskRepository = prosessTaskRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
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
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public List<BeregningSatsDto> hentGjeldendeSatser() {
        return Set.of(BeregningSatsType.ENGANG, BeregningSatsType.GRUNNBELØP, BeregningSatsType.GSNITT).stream()
                .map(beregningsresultatRepository::finnGjeldendeSats)
                .map(BeregningSatsDto::new)
                .collect(Collectors.toList());
    }

    @POST
    @Path("/satsLagreNy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagre ny sats", tags = "FORVALTNING-beregning", responses = {
            @ApiResponse(responseCode = "200", description = "Gjeldende satser", content = @Content(array = @ArraySchema(arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = BeregningSatsDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public List<BeregningSatsDto> lagreNySats(@BeanParam @Valid @NotNull BeregningSatsDto dto) {
        var type = dto.getSatsType();
        var brukTom = dto.getSatsTom() != null ? dto.getSatsTom() : LocalDate.now().plusYears(99);
        var gjeldende = beregningsresultatRepository.finnGjeldendeSats(type);
        if (!sjekkVerdierOK(dto, gjeldende, brukTom))
            throw new IllegalArgumentException("Ulovlige verdier " + dto);
        LOGGER.warn("SATSJUSTERTING: sjekk med produkteier om det er ventet, noter usedId i loggen {}", dto);
        gjeldende.setTomDato(dto.getSatsFom().minusDays(1));
        beregningsresultatRepository.lagreSats(gjeldende);
        var nysats = new BeregningSats(type, DatoIntervallEntitet.fraOgMedTilOgMed(dto.getSatsFom(), brukTom), dto.getSatsVerdi());
        beregningsresultatRepository.lagreSats(nysats);
        var nygjeldende = beregningsresultatRepository.finnGjeldendeSats(type);
        return Set.of(gjeldende, nygjeldende).stream().map(BeregningSatsDto::new).collect(Collectors.toList());
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
    @Path("/hentBeregningsgrunnlagInput")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter input for beregning", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response hentBeregningsgrunnlagInput(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        Behandling behandling = behandlingRepository.hentBehandling(dto.getBehandlingId());
        BeregningsgrunnlagInputFelles inputTjeneste = beregningsgrunnlagInputProvider.getTjeneste(behandling.getFagsakYtelseType());
        BeregningsgrunnlagInput beregningsgrunnlagInput = inputTjeneste.lagInput(behandling.getId());
        KalkulatorInputDto kalkulatorInputDto = MapTilKalkulatorInput.map(beregningsgrunnlagInput, behandling.getAktørId());
        if (kalkulatorInputDto == null) {
            return Response.noContent().build();

        }
        return Response.ok(kalkulatorInputDto).build();
    }

    /**
     *
     * Skal brukes for å feilsøke saker som kan ha blitt feilberegnet etter å ha
     * brukt feil meldekort, https://jira.adeo.no/browse/TFP-2890
     */
    @POST
    @Path("/hentMeldekortFeil")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter saker som må sjekkes for feilutbetaling grunnet feil meldekort som er brukt", tags = "FORVALTNING-beregning", responses = {
            @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = String.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response hentMeldekortFeil() {
        List<BeregningsgrunnlagGrunnlagEntitet> grunnlagList = beregningsgrunnlagRepository.hentGrunnlagForPotensielleFeilMeldekort();
        Set<Behandling> behandlinger = grunnlagList.stream()
                .filter(gr -> !besteberegningErFastsatt(gr))
                .map(BeregningsgrunnlagGrunnlagEntitet::getBehandlingId)
                .map(behandlingRepository::hentBehandling)
                .collect(Collectors.toSet());
        LOGGER.info("Fant {} behandlinger som må sjekkes", behandlinger.size());
        Set<SaksnummerEnhetDto> liste = new HashSet<>();
        for (Behandling behandling : behandlinger) {
            BeregningsgrunnlagGrunnlagEntitet grunnlagEntitet = grunnlagList.stream().filter(gr -> gr.getBehandlingId().equals(behandling.getId()))
                    .findFirst().orElseThrow();
            LocalDate skjæringstidspunkt = grunnlagEntitet.getBeregningsgrunnlag().map(BeregningsgrunnlagEntitet::getSkjæringstidspunkt)
                    .orElseThrow();
            InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
            InntektArbeidYtelseGrunnlagDto kalkGrunlag = IAYMapperTilKalkulus.mapGrunnlag(inntektArbeidYtelseGrunnlag);
            AktørId aktørId = new AktørId(behandling.getAktørId().getId());
            YtelseFilterDto ytelseFilter = new YtelseFilterDto(kalkGrunlag.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunkt);
            Set<FagsakYtelseType> ytelseTyper = Set.of(FagsakYtelseType.DAGPENGER, FagsakYtelseType.ARBEIDSAVKLARINGSPENGER);
            Optional<YtelseDto> ytelseDto = BeregningUtils.sisteVedtakFørStpForType(ytelseFilter, skjæringstidspunkt, ytelseTyper);

            if (ytelseDto.isPresent()) {
                LocalDate sisteVedtakFom = ytelseDto.get().getPeriode().getFomDato();
                List<YtelseAnvistDto> alleMeldekort = ytelseFilter.getFiltrertYtelser().stream()
                        .filter(ytelse -> ytelseTyper.contains(ytelse.getRelatertYtelseType()))
                        .flatMap(ytelse -> ytelse.getYtelseAnvist().stream()).collect(Collectors.toList());

                // Henter ut siste meldekort som startet før STP
                Optional<YtelseAnvistDto> sisteMeldekort = alleMeldekort.stream()
                        .filter(ytelseAnvist -> sisteVedtakFom.minus(MELDEKORT_PERIODE_UTV).isBefore(ytelseAnvist.getAnvistTOM()))
                        .filter(mk -> mk.getAnvistFOM().isBefore(skjæringstidspunkt))
                        .max(Comparator.comparing(YtelseAnvistDto::getAnvistFOM));

                if (sisteMeldekort.isPresent()) {
                    // Hvis dette meldekortet ikke var "helt" må saken muligens revurderes
                    boolean erMeldekortKomplett = skjæringstidspunkt.isAfter(sisteMeldekort.get().getAnvistTOM());
                    if (!erMeldekortKomplett) {
                        SaksnummerEnhetDto dto = new SaksnummerEnhetDto(behandling.getFagsak().getSaksnummer().getVerdi(),
                                behandling.getBehandlendeEnhet());
                        liste.add(dto);
                    }
                }

            }
        }
        return Response.ok(liste).build();
    }

    @POST
    @Path("/opprettGrunnbeløpForBehandling")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter grunnbeløp for behandling", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response opprettGrunnbeløpForBehandling(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        Behandling behandling = behandlingRepository.hentBehandling(dto.getBehandlingId());
        opprettTask(behandling, OpprettGrunnbeløpTask.TASKNAME);
        return Response.ok().build();
    }

    @POST
    @Path("/opprettGrunnbeløp")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter grunnbeløp der det mangler", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response opprettGrunnbeløp() {
        List<Long> behandlingIdList = beregningsgrunnlagRepository.hentBehandlingIdForGrunnlagUtenGrunnbeløp();
        behandlingIdList.forEach(id -> {
            Behandling behandling = behandlingRepository.hentBehandling(id);
            opprettTask(behandling, OpprettGrunnbeløpTask.TASKNAME);
        });
        return Response.ok().build();
    }

    private boolean besteberegningErFastsatt(BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        List<FaktaOmBeregningTilfelle> tilfeller = grunnlag.getBeregningsgrunnlag()
                .map(BeregningsgrunnlagEntitet::getFaktaOmBeregningTilfeller)
                .orElse(Collections.emptyList());
        return tilfeller.contains(FaktaOmBeregningTilfelle.FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE);
    }

    @POST
    @Path("/tilbakerullingAlleSakerBeregning")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Rull saker tilbake til beregning", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response tilbakerullAlleSakerBeregning() {
        beregningsgrunnlagRepository.opprettProsesstaskForTilbakerullingAvSakerBeregning();
        return Response.ok().build();
    }

    @POST
    @Path("/tilbakerullingBeregning")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Rull sak tilbake til beregning", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response tilbakerullEnSakBeregning(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        Behandling behandling = behandlingRepository.hentBehandling(dto.getBehandlingId());
        opprettTilbakerullingBeregningTask(behandling);
        return Response.ok().build();
    }

    private void opprettTilbakerullingBeregningTask(Behandling behandling) {
        opprettTask(behandling, TilbakerullingBeregningTask.TASKNAME);
    }

    private void opprettTask(Behandling behandling, String taskname) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(taskname);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

}
