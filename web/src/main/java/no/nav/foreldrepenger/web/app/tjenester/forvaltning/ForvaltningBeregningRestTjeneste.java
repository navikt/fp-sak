package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.task.TilbakerullingBeregningTask;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;

@Path("/forvaltningBeregning")
@ApplicationScoped
@Transaction
public class ForvaltningBeregningRestTjeneste {

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

    @POST
    @Path("/hentSVPFeil")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter SVP-saker som må sjekkes for feilutbetaling", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(action = READ, ressurs = BeskyttetRessursResourceAttributt.DRIFT, sporingslogg = false)
    public Response hentSVPFeil() {
        List<BeregningsgrunnlagGrunnlagEntitet> grunnlagList = beregningsgrunnlagRepository.hentGrunnlagForPotensielleFeilSVP();
        List<Behandling> svpBehandlinger = grunnlagList.stream().map(BeregningsgrunnlagGrunnlagEntitet::getBehandlingId).map(behandlingRepository::hentBehandling)
            .filter(b -> b.getFagsakYtelseType().equals(FagsakYtelseType.SVANGERSKAPSPENGER))
            .collect(Collectors.toList());
        List<Saksnummer> saksnummerList = new ArrayList<>();
        for (Behandling behandling : svpBehandlinger) {
            BeregningsgrunnlagGrunnlagEntitet grunnlagEntitet = grunnlagList.stream().filter(gr -> gr.getBehandlingId().equals(behandling.getId())).findFirst().orElseThrow();
            LocalDate skjæringstidspunkt = grunnlagEntitet.getBeregningsgrunnlag().map(BeregningsgrunnlagEntitet::getSkjæringstidspunkt).orElseThrow();
            InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
            List<Inntektsmelding> inntektsmeldinger = inntektArbeidYtelseGrunnlag.getInntektsmeldinger().stream().flatMap(ims -> ims.getInntektsmeldingerSomSkalBrukes().stream()).collect(Collectors.toList());
            List<Inntektsmelding> inntektsmeldingerMedStartdatoEtterStpOgRefusjon = inntektsmeldinger.stream()
                .filter(im -> im.getRefusjonBeløpPerMnd() != null && !im.getRefusjonBeløpPerMnd().erNullEllerNulltall())
                .filter(im -> im.getStartDatoPermisjon().isPresent() && im.getStartDatoPermisjon().get().isAfter(skjæringstidspunkt))
                .collect(Collectors.toList());
            BeregningsgrunnlagEntitet beregningsgrunnlagEntitet = grunnlagEntitet.getBeregningsgrunnlag().orElseThrow();
            List<BeregningsgrunnlagPrStatusOgAndel> andelerIFørstePeriode = beregningsgrunnlagEntitet.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList();
            List<BeregningsgrunnlagPrStatusOgAndel> andelerSomManglerRefusjon = andelerIFørstePeriode.stream().filter(a -> inntektsmeldingerMedStartdatoEtterStpOgRefusjon.stream().anyMatch(im -> a.gjelderSammeArbeidsforhold(im.getArbeidsgiver(), im.getArbeidsforholdRef())))
                .filter(a -> a.getBgAndelArbeidsforhold().isPresent() && a.getBgAndelArbeidsforhold().get().getRefusjonskravPrÅr() == null)
                .collect(Collectors.toList());

            if (andelerSomManglerRefusjon.size() > 0) {
                saksnummerList.add(behandling.getFagsak().getSaksnummer());
            }
        }
        return Response.ok(saksnummerList).build();
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
