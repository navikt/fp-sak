package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.RevurderingBehandlingsresultatutlederTest.ARBEIDSFORHOLDLISTE;
import static no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.RevurderingBehandlingsresultatutlederTest.ORGNR;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.vedtak.konfig.Tid;

public class ErEndringIBeregningTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = LocalDate.now();

    @Test
    public void skal_gi_ingen_endring_i_beregningsgrunnlag_ved_like_grunnlag() {
        // Arrange
        // Originalgrunnlag
        var bgOrg = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Org = byggBGPeriode(bgOrg, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50));
        var p2Org = byggBGPeriode(bgOrg, etterSTP(51), Tid.TIDENES_ENDE);
        byggAndel(p1Org, AktivitetStatus.ARBEIDSTAKER, 1500, 0);
        byggAndel(p2Org, AktivitetStatus.ARBEIDSTAKER, 1500, 0);
        BeregningsgrunnlagPeriode.oppdater(p1Org).build(bgOrg);
        BeregningsgrunnlagPeriode.oppdater(p2Org).build(bgOrg);

        // Revurderingsgrunnlag
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Rev = BeregningsgrunnlagPeriode.oppdater(byggBGPeriode(bgRev, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50))).build(bgRev);
        var p2Rev = BeregningsgrunnlagPeriode.oppdater(byggBGPeriode(bgRev, etterSTP(51), Tid.TIDENES_ENDE)).build(bgRev);
        byggAndel(p1Rev, AktivitetStatus.ARBEIDSTAKER, 1500, 0);
        byggAndel(p2Rev, AktivitetStatus.ARBEIDSTAKER, 1500, 0);
        BeregningsgrunnlagPeriode.oppdater(p1Rev).build(bgRev);
        BeregningsgrunnlagPeriode.oppdater(p2Rev).build(bgRev);

        // Act
        var endring = ErEndringIBeregning.vurder(Optional.of(bgRev), Optional.of(bgOrg));

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    public void skal_gi_endring_når_vi_mangler_beregningsgrunnlag_på_en_av_behandlingene() {
        // Arrange
        // Revurderingsgrunnlag
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Rev = BeregningsgrunnlagPeriode.oppdater(byggBGPeriode(bgRev, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50))).build(bgRev);
        var p2Rev = BeregningsgrunnlagPeriode.oppdater(byggBGPeriode(bgRev, etterSTP(51), Tid.TIDENES_ENDE)).build(bgRev);
        byggAndel(p1Rev, AktivitetStatus.ARBEIDSTAKER, 1500, 0);
        byggAndel(p2Rev, AktivitetStatus.ARBEIDSTAKER, 1500, 0);
        BeregningsgrunnlagPeriode.oppdater(p1Rev).build(bgRev);
        BeregningsgrunnlagPeriode.oppdater(p2Rev).build(bgRev);

        // Act
        var endring = ErEndringIBeregning.vurder(Optional.of(bgRev), Optional.empty());

        // Assert
        assertThat(endring).isTrue();
    }

    @Test
    public void skal_gi_ingen_endring_når_vi_mangler_begge_beregningsgrunnlag() {
        // Act
        var endring = ErEndringIBeregning.vurder(Optional.empty(), Optional.empty());

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    public void skal_gi_ingen_endring_når_vi_har_like_mange_perioder_med_med_forskjellige_fom_og_tom() {
        // Arrange
        // Originalgrunnlag
        var bgOrg = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Org = byggBGPeriode(bgOrg, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50));
        var p2Org = byggBGPeriode(bgOrg, etterSTP(51), Tid.TIDENES_ENDE);
        byggAndel(p1Org, AktivitetStatus.ARBEIDSTAKER, 1500, 500);
        byggAndel(p2Org, AktivitetStatus.ARBEIDSTAKER, 1500, 500);
        BeregningsgrunnlagPeriode.oppdater(p1Org).build(bgOrg);
        BeregningsgrunnlagPeriode.oppdater(p2Org).build(bgOrg);

        // Revurderingsgrunnlag
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Rev = BeregningsgrunnlagPeriode.oppdater(byggBGPeriode(bgRev, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(30))).build(bgRev);
        var p2Rev = BeregningsgrunnlagPeriode.oppdater(byggBGPeriode(bgRev, etterSTP(31), Tid.TIDENES_ENDE)).build(bgRev);
        byggAndel(p1Rev, AktivitetStatus.ARBEIDSTAKER, 1500, 500);
        byggAndel(p2Rev, AktivitetStatus.ARBEIDSTAKER, 1500, 500);
        BeregningsgrunnlagPeriode.oppdater(p1Rev).build(bgRev);
        BeregningsgrunnlagPeriode.oppdater(p2Rev).build(bgRev);

        // Act
        var endring = ErEndringIBeregning.vurder(Optional.of(bgRev), Optional.of(bgOrg));

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    public void skal_gi_ingen_endring_når_vi_har_like_mange_perioder_med_forskjellig_startdato() {
        // Arrange
        // Originalgrunnlag
        var bgOrg = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Org = byggBGPeriode(bgOrg, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50));
        var p2Org = byggBGPeriode(bgOrg, etterSTP(51), Tid.TIDENES_ENDE);
        byggAndel(p1Org, AktivitetStatus.ARBEIDSTAKER, 1500, 500);
        byggAndel(p2Org, AktivitetStatus.ARBEIDSTAKER, 1500, 500);
        BeregningsgrunnlagPeriode.oppdater(p1Org).build(bgOrg);
        BeregningsgrunnlagPeriode.oppdater(p2Org).build(bgOrg);

        // Revurderingsgrunnlag
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(10), AktivitetStatus.ARBEIDSTAKER);
        var p1Rev = BeregningsgrunnlagPeriode.oppdater(byggBGPeriode(bgRev, SKJÆRINGSTIDSPUNKT_BEREGNING.minusDays(10), etterSTP(30))).build(bgRev);
        var p2Rev = BeregningsgrunnlagPeriode.oppdater(byggBGPeriode(bgRev, etterSTP(31), Tid.TIDENES_ENDE)).build(bgRev);
        byggAndel(p1Rev, AktivitetStatus.ARBEIDSTAKER, 1500, 500);
        byggAndel(p2Rev, AktivitetStatus.ARBEIDSTAKER, 1500, 500);
        BeregningsgrunnlagPeriode.oppdater(p1Rev).build(bgRev);
        BeregningsgrunnlagPeriode.oppdater(p2Rev).build(bgRev);

        // Act
        var endring = ErEndringIBeregning.vurder(Optional.of(bgRev), Optional.of(bgOrg));

        // Assert
        assertThat(endring).isFalse();
    }

    @Test
    public void skal_gi_endring_i_beregningsgrunnlag_ved_ulik_dagsats_på_periodenivå() {
        // Arrange
        // Originalgrunnlag
        var bgOrg = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Org = byggBGPeriode(bgOrg, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50));
        var p2Org = byggBGPeriode(bgOrg, etterSTP(51), Tid.TIDENES_ENDE);
        byggAndel(p1Org, AktivitetStatus.ARBEIDSTAKER, 1500, 300);
        byggAndel(p2Org, AktivitetStatus.ARBEIDSTAKER, 1500, 300);
        BeregningsgrunnlagPeriode.oppdater(p1Org).build(bgOrg);
        BeregningsgrunnlagPeriode.oppdater(p2Org).build(bgOrg);

        // Revurderingsgrunnlag
        var bgRev = byggBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, AktivitetStatus.ARBEIDSTAKER);
        var p1Rev = BeregningsgrunnlagPeriode.oppdater(byggBGPeriode(bgRev, SKJÆRINGSTIDSPUNKT_BEREGNING, etterSTP(50))).build(bgRev);
        var p2Rev = BeregningsgrunnlagPeriode.oppdater(byggBGPeriode(bgRev, etterSTP(51), Tid.TIDENES_ENDE)).build(bgRev);
        byggAndel(p1Rev, AktivitetStatus.ARBEIDSTAKER, 1500, 299);
        byggAndel(p2Rev, AktivitetStatus.ARBEIDSTAKER, 1500, 299);
        BeregningsgrunnlagPeriode.oppdater(p1Rev).build(bgRev);
        BeregningsgrunnlagPeriode.oppdater(p2Rev).build(bgRev);

        // Act
        var endring = ErEndringIBeregning.vurder(Optional.of(bgRev), Optional.of(bgOrg));

        // Assert
        assertThat(endring).isTrue();
    }

    private LocalDate etterSTP(int dagerEtter) {
        return SKJÆRINGSTIDSPUNKT_BEREGNING.plusDays(dagerEtter);
    }

    private void byggAndel(BeregningsgrunnlagPeriode periode, AktivitetStatus aktivitetStatus, int dagsatsBruker, int dagsatsAG) {
        var andelBuilder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(aktivitetStatus)
            .medBeregnetPrÅr(BigDecimal.valueOf(240000))
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
                .medRedusertRefusjonPrÅr(BigDecimal.valueOf(dagsatsAG * 260L));
        }
        andelBuilder.build(periode);
    }

    public static BeregningsgrunnlagEntitet byggBeregningsgrunnlag(LocalDate skjæringstidspunktBeregning, AktivitetStatus... statuser) {
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medSkjæringstidspunkt(skjæringstidspunktBeregning)
            .medGrunnbeløp(BigDecimal.valueOf(91425L))
            .build();
        Arrays.asList(statuser).forEach(status -> BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(status)
            .build(beregningsgrunnlag));
        return beregningsgrunnlag;
    }

    private static BeregningsgrunnlagPeriode byggBGPeriode(BeregningsgrunnlagEntitet bg, LocalDate fom, LocalDate tom) {
        return BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(fom, tom)
            .build(bg);
    }
}
