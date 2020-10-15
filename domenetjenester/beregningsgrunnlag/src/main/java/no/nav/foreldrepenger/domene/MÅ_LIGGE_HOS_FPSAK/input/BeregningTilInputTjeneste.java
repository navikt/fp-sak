package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.input;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.Grunnbeløp;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.modell.behandling.KoblingReferanse;
import no.nav.folketrygdloven.kalkulator.modell.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.KalkulusKonfigInjecter;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.BehandlingslagerTilKalkulusMapper;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.typer.Beløp;

@ApplicationScoped
public class BeregningTilInputTjeneste {

    private static final String UTVIKLER_FEIL_SKAL_HA_BEREGNINGSGRUNNLAG_HER = "Utvikler-feil: skal ha beregningsgrunnlag her";
    private static final Supplier<IllegalStateException> INGEN_BG_EXCEPTION_SUPPLIER = () -> new IllegalStateException(UTVIKLER_FEIL_SKAL_HA_BEREGNINGSGRUNNLAG_HER);

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private KalkulusKonfigInjecter kalkulusKonfigInjecter;

    public BeregningTilInputTjeneste() {
        // CDI
    }

    @Inject
    public BeregningTilInputTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                     KalkulusKonfigInjecter kalkulusKonfigInjecter) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.kalkulusKonfigInjecter = kalkulusKonfigInjecter;
    }

    public BeregningsgrunnlagInput lagInputMedVerdierFraBeregning(BeregningsgrunnlagInput input) {
        return lagInputMedBeregningsgrunnlag(input);
    }

    private BeregningsgrunnlagInput lagInputMedBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        Long behandlingId = input.getKoblingReferanse().getKoblingId();
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagEntitetOpt = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
        if (grunnlagEntitetOpt.isPresent()) {
            BeregningsgrunnlagGrunnlagEntitet grunnlagEntitet = grunnlagEntitetOpt.get();
            BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlagEntitet.getBeregningsgrunnlag()
                .orElseThrow(INGEN_BG_EXCEPTION_SUPPLIER);
            var ref = oppdaterBehandlingreferanseMedSkjæringstidspunktBeregning(input.getKoblingReferanse(), grunnlagEntitet.getGjeldendeAktiviteter(), beregningsgrunnlag);
            input = input
                .medBehandlingReferanse(ref)
                .medBeregningsgrunnlagGrunnlag(BehandlingslagerTilKalkulusMapper.mapGrunnlag(grunnlagEntitet));
        }
        Optional<Long> orginalBehandling = input.getKoblingReferanse().getOriginalKoblingId();
        Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag = orginalBehandling.flatMap(beh -> beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(beh));
        if (forrigeGrunnlag.isPresent()) {
            // Trenger ikke vite hvilke ander i orginalbehandling som hadde inntektsmeldinger
            input = input
                .medBeregningsgrunnlagGrunnlagFraForrigeBehandling(BehandlingslagerTilKalkulusMapper.mapGrunnlag(forrigeGrunnlag.get()));
        }
        kalkulusKonfigInjecter.leggTilKonfigverdier(input);
        kalkulusKonfigInjecter.leggTilFeatureToggles(input);
        return input;
    }

    private KoblingReferanse oppdaterBehandlingreferanseMedSkjæringstidspunktBeregning(KoblingReferanse ref,
                                                                                          BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                                                                          BeregningsgrunnlagEntitet beregningsgrunnlag) {
        LocalDate skjæringstidspunktOpptjening = beregningAktivitetAggregat.getSkjæringstidspunktOpptjening();
        LocalDate førsteUttaksdato = ref.getFørsteUttaksdato();
        LocalDate skjæringstidspunktBeregning = beregningsgrunnlag.getSkjæringstidspunkt();
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medSkjæringstidspunktOpptjening(skjæringstidspunktOpptjening)
            .medFørsteUttaksdato(førsteUttaksdato)
            .medSkjæringstidspunktBeregning(skjæringstidspunktBeregning).build();
        return ref.medSkjæringstidspunkt(skjæringstidspunkt);
    }

}
