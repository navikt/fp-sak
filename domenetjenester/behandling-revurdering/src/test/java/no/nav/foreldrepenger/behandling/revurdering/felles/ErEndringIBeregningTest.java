package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil.ARBEIDSFORHOLDLISTE;
import static no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil.ORGNR;
import static no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil.SKJÆRINGSTIDSPUNKT_BEREGNING;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.vedtak.konfig.Tid;

class ErEndringIBeregningTest {

    @Test
    void skal_gi_ingen_endring_i_beregningsgrunnlag_ved_like_grunnlag() {
        // Arrange
        // Originalgrunnlag
        var p1Org = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50), byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 0));
        var p2Org = byggBGPeriode(etterSTP(51), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 0));
        var bgOrg = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Arrays.asList(p1Org, p2Org), AktivitetStatus.ARBEIDSTAKER);

        // Revurderingsgrunnlag
        var p1Rev = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50), byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 0));
        var p2Rev = byggBGPeriode(etterSTP(51), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 0));
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Arrays.asList(p1Rev, p2Rev), AktivitetStatus.ARBEIDSTAKER);

        // Act
        var endring = ErEndringIBeregning.vurder(Optional.of(bgRev), Optional.of(bgOrg));

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    void skal_gi_endring_når_vi_mangler_beregningsgrunnlag_på_en_av_behandlingene() {
        // Arrange
        // Revurderingsgrunnlag
        var p1Rev = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50), byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 0));
        var p2Rev = byggBGPeriode(etterSTP(51), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 0));
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Arrays.asList(p1Rev, p2Rev), AktivitetStatus.ARBEIDSTAKER);

        // Act
        var endring = ErEndringIBeregning.vurder(Optional.of(bgRev), Optional.empty());

        // Assert
        assertThat(endring).isTrue();
    }

    @Test
    void skal_gi_ingen_endring_når_vi_mangler_begge_beregningsgrunnlag() {
        // Act
        var endring = ErEndringIBeregning.vurder(Optional.empty(), Optional.empty());

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    void skal_gi_ingen_endring_når_vi_har_like_mange_perioder_med_med_forskjellige_fom_og_tom() {
        // Arrange
        // Originalgrunnlag
        var p1Org = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50), byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 500));
        var p2Org = byggBGPeriode(etterSTP(51), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 500));
        var bgOrg = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Arrays.asList(p1Org, p2Org), AktivitetStatus.ARBEIDSTAKER);

        // Revurderingsgrunnlag
        var p1Rev = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(30), byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 500));
        var p2Rev = byggBGPeriode(etterSTP(31), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 500));
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Arrays.asList(p1Rev, p2Rev), AktivitetStatus.ARBEIDSTAKER);


        // Act
        var endring = ErEndringIBeregning.vurder(Optional.of(bgRev), Optional.of(bgOrg));

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    void skal_gi_ingen_endring_når_vi_har_like_mange_perioder_med_forskjellig_startdato() {
        // Arrange
        // Originalgrunnlag
        var p1Org = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50), byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 500));
        var p2Org = byggBGPeriode(etterSTP(51), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 500));
        var bgOrg = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Arrays.asList(p1Org, p2Org), AktivitetStatus.ARBEIDSTAKER);

        // Revurderingsgrunnlag
        var p1Rev = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(10), etterSTP(30), byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 500));
        var p2Rev = byggBGPeriode(etterSTP(31), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 500));
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(10), Arrays.asList(p1Rev, p2Rev), AktivitetStatus.ARBEIDSTAKER);

        // Act
        var endring = ErEndringIBeregning.vurder(Optional.of(bgRev), Optional.of(bgOrg));

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    void skal_gi_endring_i_beregningsgrunnlag_ved_ulik_dagsats_på_periodenivå() {
        // Arrange
        // Originalgrunnlag
        var p1Org = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50), byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 300));
        var p2Org = byggBGPeriode(etterSTP(51), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 300));
        var bgOrg = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Arrays.asList(p1Org, p2Org), AktivitetStatus.ARBEIDSTAKER);

        // Revurderingsgrunnlag
        var p1Rev = byggBGPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50), byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 299));
        var p2Rev = byggBGPeriode(etterSTP(51), Tid.TIDENES_ENDE, byggAndel(AktivitetStatus.ARBEIDSTAKER, 1500, 299));
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, Arrays.asList(p1Rev, p2Rev), AktivitetStatus.ARBEIDSTAKER);

        // Act
        var endring = ErEndringIBeregning.vurder(Optional.of(bgRev), Optional.of(bgOrg));

        // Assert
        assertThat(endring).isTrue();
    }

    private LocalDate etterSTP(int dagerEtter) {
        return SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(dagerEtter);
    }

    private BeregningsgrunnlagPrStatusOgAndel byggAndel(AktivitetStatus aktivitetStatus, int dagsatsBruker, int dagsatsAG) {
        var andelBuilder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(aktivitetStatus)
            .medKilde(AndelKilde.PROSESS_START)
            .medBeregnetPrÅr(BigDecimal.valueOf(240000))
            .medBruttoPrÅr(BigDecimal.valueOf(240000))
            .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(dagsatsBruker * 260L));
        if (aktivitetStatus.erArbeidstaker()) {
            var bga = BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medArbeidsforholdRef(ARBEIDSFORHOLDLISTE.get(0))
                .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
                .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
            andelBuilder
                .medBGAndelArbeidsforhold(bga)
                .medRedusertRefusjonPrÅr(BigDecimal.valueOf(dagsatsAG * 260L))
                .medDagsatsArbeidsgiver((long) dagsatsAG);
        }
        return andelBuilder.build();
    }

    public static Beregningsgrunnlag byggBeregningsgrunnlag(LocalDate skjæringstidspunktBeregning, List<BeregningsgrunnlagPeriode> perioder, AktivitetStatus... statuser) {
        var bgBuilder = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(skjæringstidspunktBeregning)
            .medGrunnbeløp(BigDecimal.valueOf(91425L));
        Arrays.asList(statuser).forEach(status -> bgBuilder.leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(status)
            .build()));
        perioder.forEach(bgBuilder::leggTilBeregningsgrunnlagPeriode);
        return bgBuilder.build();
    }

    private static BeregningsgrunnlagPeriode byggBGPeriode(LocalDate fom, LocalDate tom, BeregningsgrunnlagPrStatusOgAndel... andeler) {
        var bgAndeler = Arrays.asList(andeler);
        var dagsatsPeriode = bgAndeler.stream().map(BeregningsgrunnlagPrStatusOgAndel::getDagsats).reduce(Long::sum).orElse(null);
        var periodeBuilder = BeregningsgrunnlagPeriode.builder().medBeregningsgrunnlagPeriode(fom, tom).medDagsats(dagsatsPeriode);
        bgAndeler.forEach(periodeBuilder::leggTilBeregningsgrunnlagPrStatusOgAndel);
        return periodeBuilder.build();
    }
}
