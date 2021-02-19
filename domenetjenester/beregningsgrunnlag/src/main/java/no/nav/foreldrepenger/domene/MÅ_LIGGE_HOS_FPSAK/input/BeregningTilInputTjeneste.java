package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.input;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.modell.behandling.KoblingReferanse;
import no.nav.folketrygdloven.kalkulator.modell.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.KalkulusKonfigInjecter;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.BehandlingslagerTilKalkulusMapper;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;

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
