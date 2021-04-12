package no.nav.foreldrepenger.behandling.revurdering.felles;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

public class ErEndringIBeregningTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = LocalDate.now();

    @Test
    public void skal_gi_ingen_endring_i_beregningsgrunnlag_ved_lik_dagsats_på_periodenoivå() {
        // Arrange
        List<ÅpenDatoIntervallEntitet> bgPeriode = Collections.singletonList(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        BeregningsgrunnlagEntitet originalGrunnlag = byggOgLagreBeregningsgrunnlagForBehandling(
                false, true, bgPeriode);
        BeregningsgrunnlagEntitet revurderingGrunnlag = byggOgLagreBeregningsgrunnlagForBehandling(false, true, bgPeriode);

        // Act
        boolean endring = ErEndringIBeregning.vurder(Optional.of(revurderingGrunnlag), Optional.of(originalGrunnlag));

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    public void skal_gi_endring_når_vi_mangler_beregningsgrunnlag_på_en_av_behandlingene() {
        // Arrange
        List<ÅpenDatoIntervallEntitet> bgPeriode = Collections.singletonList(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        BeregningsgrunnlagEntitet revurderingGrunnlag = byggOgLagreBeregningsgrunnlagForBehandling(false, true,
                bgPeriode);

        // Act
        boolean endring = ErEndringIBeregning.vurder(Optional.of(revurderingGrunnlag), Optional.empty());

        // Assert
        assertThat(endring).isTrue();
    }

    @Test
    public void skal_gi_ingen_endring_når_vi_mangler_begge_beregningsgrunnlag() {
        // Act
        boolean endring = ErEndringIBeregning.vurder(Optional.empty(), Optional.empty());

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    public void skal_gi_ingen_endring_når_vi_har_like_mange_perioder_med_med_forskjellige_fom_og_tom() {
        // Arrange
        List<ÅpenDatoIntervallEntitet> bgPerioderNyttGrunnlag = List.of(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING,
                        SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(35)),
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(36), null));
        List<ÅpenDatoIntervallEntitet> bgPerioderOriginaltGrunnlag = List.of(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING,
                        SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(40)),
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(41), null));
        BeregningsgrunnlagEntitet originalGrunnlag = byggOgLagreBeregningsgrunnlagForBehandling(false, true, bgPerioderOriginaltGrunnlag);
        BeregningsgrunnlagEntitet revurderingGrunnlag = byggOgLagreBeregningsgrunnlagForBehandling(false, true,
                bgPerioderNyttGrunnlag);

        // Act
        boolean endring = ErEndringIBeregning.vurder(Optional.of(revurderingGrunnlag), Optional.of(originalGrunnlag));

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    public void skal_gi_ingen_endring_når_vi_har_like_mange_perioder_med_forskjellig_startdato() {
        // Arrange
        List<ÅpenDatoIntervallEntitet> bgPerioderNyttGrunnlag = List.of(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(1),
                        SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(35)),
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(36), null));
        List<ÅpenDatoIntervallEntitet> bgPerioderOriginaltGrunnlag = List.of(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING,
                        SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(40)),
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(41), null));
        BeregningsgrunnlagEntitet originalGrunnlag = byggOgLagreBeregningsgrunnlagForBehandling(false, true, bgPerioderOriginaltGrunnlag);
        BeregningsgrunnlagEntitet revurderingGrunnlag = byggOgLagreBeregningsgrunnlagForBehandling(false, true,
                bgPerioderNyttGrunnlag);

        // Act
        boolean endring = ErEndringIBeregning.vurder(Optional.of(revurderingGrunnlag), Optional.of(originalGrunnlag));

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    public void skal_gi_endring_i_beregningsgrunnlag_ved_ulik_dagsats_på_periodenoivå() {
        // Arrange
        List<ÅpenDatoIntervallEntitet> bgPeriode = Collections.singletonList(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        BeregningsgrunnlagEntitet originalGrunnlag = byggOgLagreBeregningsgrunnlagForBehandling(false, true, bgPeriode);
        BeregningsgrunnlagEntitet revurderingGrunnlag = byggOgLagreBeregningsgrunnlagForBehandling(true, true,
                bgPeriode);

        // Act
        boolean endring = ErEndringIBeregning.vurder(Optional.of(revurderingGrunnlag), Optional.of(originalGrunnlag));

        // Assert
        assertThat(endring).isTrue();
    }

    @Test
    public void skal_ikke_gi_ugunst_når_samme_beregningsgrunnlag() {
        BeregningsgrunnlagEntitet revurderingGrunnlag = byggBeregningsgrunnlag();
        BeregningsgrunnlagEntitet originaltGrunnlag = byggBeregningsgrunnlag();
        byggPeriode(revurderingGrunnlag, SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(1), 1000L);
        byggPeriode(originaltGrunnlag, SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(1), 1000L);

        boolean erUgunst = ErEndringIBeregning.vurderUgunst(Optional.of(revurderingGrunnlag), Optional.of(originaltGrunnlag), SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(10));

        assertThat(erUgunst).isFalse();
    }

    @Test
    public void skal_gi_ugunst_når_revurdering_har_mindre_beregningsgrunnlag() {
        BeregningsgrunnlagEntitet revurderingGrunnlag = byggBeregningsgrunnlag();
        BeregningsgrunnlagEntitet originaltGrunnlag = byggBeregningsgrunnlag();
        byggPeriode(revurderingGrunnlag, SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(1), 900L);
        byggPeriode(originaltGrunnlag, SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(1), 1000L);

        boolean erUgunst = ErEndringIBeregning.vurderUgunst(Optional.of(revurderingGrunnlag), Optional.of(originaltGrunnlag), SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(10));


        assertThat(erUgunst).isTrue();
    }

    @Test
    public void skal_ikke_gi_ugunst_når_revurdering_har_mindre_beregningsgrunnlag_etter_siste_uttaksdato() {
        BeregningsgrunnlagEntitet revurderingGrunnlag = byggBeregningsgrunnlag();
        BeregningsgrunnlagEntitet originaltGrunnlag = byggBeregningsgrunnlag();
        byggPeriode(revurderingGrunnlag, SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(10), 1000L);
        byggPeriode(revurderingGrunnlag, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(11), SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(15), 900L);
        byggPeriode(originaltGrunnlag, SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(10), 1000L);
        byggPeriode(originaltGrunnlag, SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(11), SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(15), 1000L);

        boolean erUgunst = ErEndringIBeregning.vurderUgunst(Optional.of(revurderingGrunnlag), Optional.of(originaltGrunnlag), SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(10));

        assertThat(erUgunst).isFalse();
    }

    private BeregningsgrunnlagEntitet byggOgLagreBeregningsgrunnlagForBehandling(boolean medOppjustertDagsat,
                                                                                 boolean skalDeleAndelMellomArbeidsgiverOgBruker,
                                                                                 List<ÅpenDatoIntervallEntitet> perioder) {
        return byggOgLagreBeregningsgrunnlagForBehandling(medOppjustertDagsat,
                skalDeleAndelMellomArbeidsgiverOgBruker, perioder, new LagEnAndelTjeneste());
    }

    private BeregningsgrunnlagEntitet byggOgLagreBeregningsgrunnlagForBehandling(boolean medOppjustertDagsat,
                                                                                 boolean skalDeleAndelMellomArbeidsgiverOgBruker,
                                                                                 List<ÅpenDatoIntervallEntitet> perioder,
                                                                                 LagAndelTjeneste lagAndelTjeneste) {
        return LagBeregningsgrunnlagTjeneste.lagBeregningsgrunnlag(
                SKJÆRINGSTIDSPUNKT_BEREGNING, medOppjustertDagsat, skalDeleAndelMellomArbeidsgiverOgBruker, perioder,
                lagAndelTjeneste);
    }

    private BeregningsgrunnlagEntitet byggBeregningsgrunnlag() {
        return BeregningsgrunnlagEntitet.ny()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .medGrunnbeløp(BigDecimal.valueOf(91425))
            .build();
    }

    private void byggPeriode(BeregningsgrunnlagEntitet beregningsgrunnlag, LocalDate fom, LocalDate tom, Long bruttoPerÅr) {
        BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(fom, tom)
            .medBruttoPrÅr(BigDecimal.valueOf(bruttoPerÅr))
            .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
                .medRedusertRefusjonPrÅr(BigDecimal.valueOf(bruttoPerÅr)))
            .build(beregningsgrunnlag);
    }

}
