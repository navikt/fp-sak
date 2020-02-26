package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.task;


import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.FastsettBeregningAktiviteter;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetAggregatDto;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.ProsesseringAsynkTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.fra_kalkulus.KalkulusTilBehandlingslagerMapper;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(OpprettRegisterAktitviteterTask.TASKNAME)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettRegisterAktitviteterTask implements ProsessTaskHandler {

    public static final String TASKNAME = "beregning.opprettRegisterAktiviteter";

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BeregningsgrunnlagInputProvider inputProvider;



    OpprettRegisterAktitviteterTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettRegisterAktitviteterTask(BehandlingRepository behandlingRepository,
                                           BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                           BeregningsgrunnlagInputProvider inputProvider) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.inputProvider = inputProvider;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long behandlingId = prosessTaskData.getBehandlingId();
        beregningsgrunnlagRepository.hentGrunnlagUtenRegisterForBehandling(behandlingId).forEach(grunnlag -> {
            BeregningsgrunnlagGrunnlagBuilder oppdatere = BeregningsgrunnlagGrunnlagBuilder.oppdatere(grunnlag);
            Optional<BeregningsgrunnlagGrunnlagEntitet> aktivtGrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
            Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagMedRegister = aktivtGrunnlag.filter(gr -> gr.getRegisterAktiviteter() != null);
            if (grunnlagMedRegister.isPresent()) {
                BeregningAktivitetAggregatEntitet registerAktiviteter = grunnlagMedRegister.get().getRegisterAktiviteter();
                oppdatere.medRegisterAktiviteter(registerAktiviteter);
            } else {
                Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
                FagsakYtelseType fagsakYtelseType = behandling.getFagsakYtelseType();
                BeregningsgrunnlagInput beregningsgrunnlagInput = inputProvider.getTjeneste(fagsakYtelseType).lagInput(behandlingId);
                BeregningAktivitetAggregatDto beregningAktivitetAggregatDto = FastsettBeregningAktiviteter.fastsettAktiviteter(beregningsgrunnlagInput);
                BeregningAktivitetAggregatEntitet registerAktiviteter = KalkulusTilBehandlingslagerMapper.mapRegisterAktiviteter(beregningAktivitetAggregatDto);
                oppdatere.medRegisterAktiviteter(registerAktiviteter);
            }
            beregningsgrunnlagRepository.lagreUtenAktivt(behandlingId, oppdatere, grunnlag.getBeregningsgrunnlagTilstand());
        });
    }

}
