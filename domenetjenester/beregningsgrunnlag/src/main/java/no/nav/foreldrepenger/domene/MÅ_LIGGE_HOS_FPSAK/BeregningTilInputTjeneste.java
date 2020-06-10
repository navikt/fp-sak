package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.Grunnbeløp;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.modell.behandling.BehandlingReferanse;
import no.nav.folketrygdloven.kalkulator.modell.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.BehandlingslagerTilKalkulusMapper;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;

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
    public BeregningTilInputTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository, KalkulusKonfigInjecter kalkulusKonfigInjecter) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.kalkulusKonfigInjecter = kalkulusKonfigInjecter;
    }

    public BeregningsgrunnlagInput lagInputMedVerdierFraBeregning(BeregningsgrunnlagInput input) {
        return lagInputMedBeregningsgrunnlag(input);
    }

    private BeregningsgrunnlagInput lagInputMedBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        Long behandlingId = input.getBehandlingReferanse().getBehandlingId();
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagEntitetOpt = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
        BeregningsgrunnlagInput newInput = lagInputMedGrunnbeløpSatser(input);
        if (grunnlagEntitetOpt.isPresent()) {
            BeregningsgrunnlagGrunnlagEntitet grunnlagEntitet = grunnlagEntitetOpt.get();
            BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlagEntitet.getBeregningsgrunnlag()
                .orElseThrow(INGEN_BG_EXCEPTION_SUPPLIER);
            var ref = oppdaterBehandlingreferanseMedSkjæringstidspunktBeregning(input.getBehandlingReferanse(), grunnlagEntitet.getGjeldendeAktiviteter(), beregningsgrunnlag);
            newInput = newInput
                .medBehandlingReferanse(ref)
                .medBeregningsgrunnlagGrunnlag(BehandlingslagerTilKalkulusMapper.mapGrunnlag(grunnlagEntitet, input.getInntektsmeldinger()));
        }
        kalkulusKonfigInjecter.leggTilKonfigverdier(input);
        kalkulusKonfigInjecter.leggTilFeatureToggles(input);
        return lagBeregningsgrunnlagHistorikk(newInput);
    }


    private BeregningsgrunnlagInput lagBeregningsgrunnlagHistorikk(BeregningsgrunnlagInput input) {
        BeregningsgrunnlagTilstand[] tilstander = BeregningsgrunnlagTilstand.values();
        for (BeregningsgrunnlagTilstand tilstand : tilstander) {
            Optional<BeregningsgrunnlagGrunnlagEntitet> sisteBg = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(input.getBehandlingReferanse().getBehandlingId(), input.getBehandlingReferanse().getOriginalBehandlingId(), tilstand);
            sisteBg.ifPresent(gr -> input.leggTilBeregningsgrunnlagIHistorikk(BehandlingslagerTilKalkulusMapper.mapGrunnlag(gr, input.getInntektsmeldinger()),
                no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.BeregningsgrunnlagTilstand.fraKode(tilstand.getKode())));
        }
        return input;
    }

    private BeregningsgrunnlagInput lagInputMedGrunnbeløpSatser(BeregningsgrunnlagInput input) {
        var tjeneste = FagsakYtelseTypeRef.Lookup.find(GrunnbeløpTjeneste.class, input.getFagsakYtelseType()).orElseThrow();
        List<Grunnbeløp> grunnbeløpSatser = tjeneste.mapGrunnbeløpSatser();
        Integer antallGrunnbeløpMilitærHarKravPå = tjeneste.finnAntallGrunnbeløpMilitærHarKravPå();
        BeregningsgrunnlagInput newInput = input.medGrunnbeløpsatser(grunnbeløpSatser);
        newInput.getYtelsespesifiktGrunnlag()
            .setGrunnbeløpMilitærHarKravPå(antallGrunnbeløpMilitærHarKravPå);
        return newInput;
    }


    private BehandlingReferanse oppdaterBehandlingreferanseMedSkjæringstidspunktBeregning(BehandlingReferanse ref,
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
