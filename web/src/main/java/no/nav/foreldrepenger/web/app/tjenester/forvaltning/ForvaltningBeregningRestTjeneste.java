package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.folketrygdloven.kalkulator.felles.BeregningUtils;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektArbeidYtelseGrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseAnvistDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseFilterDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.AktørId;
import no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.FagsakYtelseType;
import no.nav.folketrygdloven.kalkulator.felles.BeregningUtils;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektArbeidYtelseGrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseAnvistDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseFilterDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.AktørId;
import no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.FagsakYtelseType;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.task.OpprettGrunnbeløpTask;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.task.TilbakerullingBeregningTask;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.IAYMapperTilKalkulus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/forvaltningBeregning")
@ApplicationScoped
@Transaction
public class ForvaltningBeregningRestTjeneste {
    private static final Period MELDEKORT_PERIODE_UTV = Period.parse("P30D");
    private static final Logger logger = LoggerFactory.getLogger(ForvaltningBeregningRestTjeneste.class);

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Inject
    public ForvaltningBeregningRestTjeneste(
        BeregningsgrunnlagRepository beregningsgrunnlagRepository, BehandlingRepository behandlingRepository,
        ProsessTaskRepository prosessTaskRepository, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public ForvaltningBeregningRestTjeneste() {
        //CDI
    }

    /**
     *
     * Skal brukes for å feilsøke saker som kan ha blitt feilberegnet etter å ha brukt feil meldekort, https://jira.adeo.no/browse/TFP-2890
     */
    @POST
    @Path("/hentMeldekortFeil")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter saker som må sjekkes for feilutbetaling grunnet feil meldekort som er brukt", tags = "FORVALTNING-beregning",
        responses = {
            @ApiResponse(responseCode = "200",
                content = @Content(
                    array = @ArraySchema(
                        uniqueItems = true,
                        arraySchema = @Schema(implementation = List.class),
                        schema = @Schema(implementation = String.class)),
                    mediaType = MediaType.APPLICATION_JSON
                )
            )
        }
    )
    @BeskyttetRessurs(action = READ, ressurs = BeskyttetRessursResourceAttributt.DRIFT, sporingslogg = false)
    public Response hentMeldekortFeil() {
        List<BeregningsgrunnlagGrunnlagEntitet> grunnlagList = beregningsgrunnlagRepository.hentGrunnlagForPotensielleFeilMeldekort();
        Set<Behandling> behandlinger = grunnlagList.stream()
            .filter(gr -> !besteberegningErFastsatt(gr))
            .map(BeregningsgrunnlagGrunnlagEntitet::getBehandlingId)
            .map(behandlingRepository::hentBehandling)
            .collect(Collectors.toSet());
        logger.info("Fant {} behandlinger som må sjekkes", behandlinger.size());
        Set<String> saksnummer = new HashSet<>();
        for (Behandling behandling : behandlinger) {
            BeregningsgrunnlagGrunnlagEntitet grunnlagEntitet = grunnlagList.stream().filter(gr -> gr.getBehandlingId().equals(behandling.getId())).findFirst().orElseThrow();
            LocalDate skjæringstidspunkt = grunnlagEntitet.getBeregningsgrunnlag().map(BeregningsgrunnlagEntitet::getSkjæringstidspunkt).orElseThrow();
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
                        saksnummer.add(behandling.getFagsak().getSaksnummer().getVerdi());
                    }
                }

            }
        }
        return Response.ok(saksnummer).build();
    }

    @POST
    @Path("/opprettGrunnbeløpForBehandling")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter grunnbeløp for behandling", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(action = READ, ressurs = BeskyttetRessursResourceAttributt.DRIFT, sporingslogg = false)
    public Response opprettGrunnbeløpForBehandling(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        Behandling behandling = behandlingRepository.hentBehandling(dto.getBehandlingId());
        opprettTask(behandling, OpprettGrunnbeløpTask.TASKNAME);
        return Response.ok().build();
    }

    @POST
    @Path("/opprettGrunnbeløp")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter grunnbeløp der det mangler", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(action = READ, ressurs = BeskyttetRessursResourceAttributt.DRIFT, sporingslogg = false)
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
    @BeskyttetRessurs(action = READ, ressurs = BeskyttetRessursResourceAttributt.DRIFT, sporingslogg = false)
    public Response tilbakerullAlleSakerBeregning() {
        beregningsgrunnlagRepository.opprettProsesstaskForTilbakerullingAvSakerBeregning();
        return Response.ok().build();
    }

    @POST
    @Path("/tilbakerullingBeregning")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Rull sak tilbake til beregning", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(action = READ, ressurs = BeskyttetRessursResourceAttributt.DRIFT, sporingslogg = false)
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
