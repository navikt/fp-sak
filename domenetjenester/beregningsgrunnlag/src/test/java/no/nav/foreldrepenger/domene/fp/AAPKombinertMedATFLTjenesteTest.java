package no.nav.foreldrepenger.domene.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;

@ExtendWith(MockitoExtension.class)
class AAPKombinertMedATFLTjenesteTest {

    private static final LocalDate STP_DATO = LocalDate.of(2024, 6, 1);
    private static final Skjæringstidspunkt STP = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(STP_DATO).build();

    @Mock
    private BeregningTjeneste beregningTjeneste;

    private AAPKombinertMedATFLTjeneste aapKombinertMedATFLTjeneste;
    private BehandlingReferanse behandlingReferanse;

    @BeforeEach
    void setUp() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        behandlingReferanse = BehandlingReferanse.fra(behandling);
        aapKombinertMedATFLTjeneste = new AAPKombinertMedATFLTjeneste(beregningTjeneste);
        when(beregningTjeneste.hent(any())).thenReturn(Optional.empty());
    }

    @Test
    void skal_gi_true_når_aap_og_arbeidstaker_på_stp() {
        var bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(STP_DATO)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER).build())
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).build())
            .build();
        var grunnlag = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(bg).build(BeregningsgrunnlagTilstand.FORESLÅTT);
        when(beregningTjeneste.hent(any())).thenReturn(Optional.of(grunnlag));

        assertThat(aapKombinertMedATFLTjeneste.harAAPKombinertMedATFL(behandlingReferanse, STP)).isTrue();
    }

    @Test
    void skal_gi_true_når_aap_og_frilans_på_stp() {
        var bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(STP_DATO)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER).build())
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.FRILANSER).build())
            .build();
        var grunnlag = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(bg).build(BeregningsgrunnlagTilstand.FORESLÅTT);
        when(beregningTjeneste.hent(any())).thenReturn(Optional.of(grunnlag));

        assertThat(aapKombinertMedATFLTjeneste.harAAPKombinertMedATFL(behandlingReferanse, STP)).isTrue();
    }

    @Test
    void skal_gi_false_når_kun_aap_ingen_atfl() {
        var bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(STP_DATO)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER).build())
            .build();
        var grunnlag = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(bg).build(BeregningsgrunnlagTilstand.FORESLÅTT);
        when(beregningTjeneste.hent(any())).thenReturn(Optional.of(grunnlag));

        assertThat(aapKombinertMedATFLTjeneste.harAAPKombinertMedATFL(behandlingReferanse, STP)).isFalse();
    }

    @Test
    void skal_gi_false_når_kun_atfl_ingen_aap() {
        var bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(STP_DATO)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).build())
            .build();
        var grunnlag = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(bg).build(BeregningsgrunnlagTilstand.FORESLÅTT);
        when(beregningTjeneste.hent(any())).thenReturn(Optional.of(grunnlag));

        assertThat(aapKombinertMedATFLTjeneste.harAAPKombinertMedATFL(behandlingReferanse, STP)).isFalse();
    }

    @Test
    void skal_gi_false_når_beregningsgrunnlag_er_overstyrt() {
        var bg = Beregningsgrunnlag.builder().medSkjæringstidspunkt(STP_DATO).medOverstyring(true).build();
        var overstyrtGrunnlag = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(bg).build(BeregningsgrunnlagTilstand.FORESLÅTT);
        when(beregningTjeneste.hent(any())).thenReturn(Optional.of(overstyrtGrunnlag));

        assertThat(aapKombinertMedATFLTjeneste.harAAPKombinertMedATFL(behandlingReferanse, STP)).isFalse();
    }
}
