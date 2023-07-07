package no.nav.foreldrepenger.domene.input;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.modell.behandling.KoblingReferanse;
import no.nav.folketrygdloven.kalkulator.modell.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.BehandlingslagerTilKalkulusMapper;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.prosess.KalkulusKonfigInjecter;

@ApplicationScoped
public class BeregningTilInputTjeneste {

    private static final String UTVIKLER_FEIL_SKAL_HA_BEREGNINGSGRUNNLAG_HER = "Utvikler-feil: skal ha beregningsgrunnlag her";
    private static final Supplier<IllegalStateException> INGEN_BG_EXCEPTION_SUPPLIER = () -> new IllegalStateException(
        UTVIKLER_FEIL_SKAL_HA_BEREGNINGSGRUNNLAG_HER);

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
        var behandlingId = input.getKoblingReferanse().getKoblingId();
        var grunnlagEntitetOpt = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
        if (grunnlagEntitetOpt.isPresent()) {
            var grunnlagEntitet = grunnlagEntitetOpt.get();
            var beregningsgrunnlag = grunnlagEntitet.getBeregningsgrunnlag().orElseThrow(INGEN_BG_EXCEPTION_SUPPLIER);
            var ref = oppdaterBehandlingreferanseMedSkjæringstidspunktBeregning(input.getKoblingReferanse(),
                grunnlagEntitet.getGjeldendeAktiviteter(), beregningsgrunnlag);
            input = input.medBehandlingReferanse(ref)
                .medBeregningsgrunnlagGrunnlag(BehandlingslagerTilKalkulusMapper.mapGrunnlag(grunnlagEntitet));
        }
        kalkulusKonfigInjecter.leggTilKonfigverdier(input);
        kalkulusKonfigInjecter.leggTilFeatureToggles(input);
        return input;
    }

    private KoblingReferanse oppdaterBehandlingreferanseMedSkjæringstidspunktBeregning(KoblingReferanse ref,
                                                                                       BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                                                                       BeregningsgrunnlagEntitet beregningsgrunnlag) {
        var skjæringstidspunktOpptjening = beregningAktivitetAggregat.getSkjæringstidspunktOpptjening();
        var førsteUttaksdato = ref.getFørsteUttaksdato();
        var skjæringstidspunktBeregning = beregningsgrunnlag.getSkjæringstidspunkt();
        var skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medSkjæringstidspunktOpptjening(skjæringstidspunktOpptjening)
            .medFørsteUttaksdato(førsteUttaksdato)
            .medSkjæringstidspunktBeregning(skjæringstidspunktBeregning)
            .build();
        return ref.medSkjæringstidspunkt(skjæringstidspunkt);
    }

}
