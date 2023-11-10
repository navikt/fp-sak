package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class VurderBehovForÅHindreTilbaketrekkTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019, Month.JANUARY, 20);
    private static final LocalDate ANDRE_PERIODE_FOM = SKJÆRINGSTIDSPUNKT.plusMonths(5);
    private static final LocalDate SISTE_UTTAKSDAG = SKJÆRINGSTIDSPUNKT.plusMonths(9);

    private static final Arbeidsgiver ARBEIDSGIVER1 = Arbeidsgiver.virksomhet("900050001");
    private static final Arbeidsgiver ARBEIDSGIVER2 = Arbeidsgiver.virksomhet(KUNSTIG_ORG);

    private static final InternArbeidsforholdRef REF1 = InternArbeidsforholdRef.nyRef();
    private static final InternArbeidsforholdRef REF2 = InternArbeidsforholdRef.nyRef();
    private static final InternArbeidsforholdRef REF3 = InternArbeidsforholdRef.nyRef();

    private static final LocalDate DAGENS_DATO = LocalDate.of(2019, Month.FEBRUARY, 4);

    @Test
    void ingenEndringSkalGiEmpty() {
        // Arrange
        var inntektBeløp = 1000;
        var forrigeTY = lagBeregningsresultatFP(0, inntektBeløp);
        var denneTY = lagBeregningsresultatFP(0, inntektBeløp);
        var forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        var denneTYPerioder = denneTY.getBeregningsresultatPerioder();
        var brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinjeTest(
                forrigeTYPerioder,
                denneTYPerioder, DAGENS_DATO);

        // Act
        var resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void økningIRefusjonOgReduksjonFraBrukerSkalGiEndringsdato() {
        // Arrange
        var forrigeTY = lagBeregningsresultatFP(200, 800);
        var denneTY = lagBeregningsresultatFP(0, 1000);
        var forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        var denneTYPerioder = denneTY.getBeregningsresultatPerioder();

        // Act
        var resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(MapBRAndelSammenligningTidslinje.opprettTidslinjeTest(
                forrigeTYPerioder,
                denneTYPerioder, DAGENS_DATO), List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void ingenRefusjonIForrigeOgFullRefusjonIRevurderingSkalGiEndringsdato() {
        // Arrange
        var forrigeTY = lagBeregningsresultatFP(1000, 0);
        var denneTY = lagBeregningsresultatFP(0, 1000);
        var forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        var denneTYPerioder = denneTY.getBeregningsresultatPerioder();
        var brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinjeTest(
                forrigeTYPerioder,
                denneTYPerioder, DAGENS_DATO);

        // Act
        var resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void ingenTilkjentYtelseIRevurderingSkalGiEmpty() {
        // Arrange
        var forrigeTY = lagBeregningsresultatFP(0, 1000);
        var forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> denneTYPerioder = Collections.emptyList();
        var brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinjeTest(
                forrigeTYPerioder,
                denneTYPerioder, DAGENS_DATO);

        // Act
        var resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void økningSkalGiEmpty() {
        // Arrange
        var forrigeTY = lagBeregningsresultatFP(0, 1000);
        var denneTY = lagBeregningsresultatFP(200, 800);
        var forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        var denneTYPerioder = denneTY.getBeregningsresultatPerioder();
        var brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinjeTest(
                forrigeTYPerioder,
                denneTYPerioder, DAGENS_DATO);

        // Act
        var resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void reduksjonEtterUtbetaltTomSkalGiEmpty() {
        // Arrange
        var forrigeTY = lagBeregningsresultatFP(200, 800);

        var br = BeregningsresultatEntitet.builder()
                .medRegelSporing("regelsporing")
                .medRegelInput("regelinput")
                .build();

        var periode1 = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
                .build(br);
        lagAndel(periode1, ARBEIDSGIVER1, true, 200);
        lagAndel(periode1, ARBEIDSGIVER1, false, 800);
        var periode2 = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
                .build(br);
        lagAndel(periode2, ARBEIDSGIVER1, true, 0);
        lagAndel(periode2, ARBEIDSGIVER1, false, 1000);
        var denneTY = periode1.getBeregningsresultat();

        var forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        var denneTYPerioder = denneTY.getBeregningsresultatPerioder();
        var brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinjeTest(
                forrigeTYPerioder,
                denneTYPerioder, DAGENS_DATO);

        // Act
        var resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void bortfaltAndelSkalIkkeGiAksjonspunkt() {
        // Arrange
        var originalBR = BeregningsresultatEntitet.builder()
                .medRegelSporing("regelsporing")
                .medRegelInput("regelinput")
                .build();

        var originalPeriode1 = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
                .build(originalBR);
        lagAndel(originalPeriode1, ARBEIDSGIVER1, true, 200);
        lagAndel(originalPeriode1, ARBEIDSGIVER1, false, 800);
        var originalPeriode2 = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
                .build(originalBR);
        lagAndel(originalPeriode2, ARBEIDSGIVER1, true, 0);
        lagAndel(originalPeriode2, ARBEIDSGIVER1, false, 1000);

        var revurderingBR = BeregningsresultatEntitet.builder()
                .medRegelSporing("regelsporing")
                .medRegelInput("regelinput")
                .build();

        var revurderingPeriode1 = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
                .build(revurderingBR);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER2, true, 200);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER2, false, 800);
        var revurderingPeriode2 = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
                .build(revurderingBR);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, true, 0);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, false, 1000);

        var forrigeTYPerioder = originalBR.getBeregningsresultatPerioder();
        var denneTYPerioder = revurderingBR.getBeregningsresultatPerioder();
        var brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinjeTest(
                forrigeTYPerioder,
                denneTYPerioder, DAGENS_DATO);

        // Act
        var resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void originalAndelUtenReferanseOgRefusjonSplittetTilToAndelerMedReferanseOgRefusjon() {
        // Arrange
        var originalBR = BeregningsresultatEntitet.builder()
                .medRegelSporing("regelsporing")
                .medRegelInput("regelinput")
                .build();

        var originalPeriode1 = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
                .build(originalBR);
        lagAndel(originalPeriode1, ARBEIDSGIVER1, true, 1000, null);
        var originalPeriode2 = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
                .build(originalBR);
        lagAndel(originalPeriode2, ARBEIDSGIVER1, true, 1000, null);

        var revurderingBR = BeregningsresultatEntitet.builder()
                .medRegelSporing("regelsporing")
                .medRegelInput("regelinput")
                .build();

        var revurderingPeriode1 = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
                .build(revurderingBR);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER1, true, 0, REF1);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER1, false, 500, REF1);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER1, true, 0, REF2);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER1, false, 500, REF2);
        var revurderingPeriode2 = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
                .build(revurderingBR);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, true, 0, REF1);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, false, 500, REF1);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, true, 0, REF2);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, false, 500, REF2);

        var forrigeTYPerioder = originalBR.getBeregningsresultatPerioder();
        var denneTYPerioder = revurderingBR.getBeregningsresultatPerioder();
        var brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinjeTest(
                forrigeTYPerioder,
                denneTYPerioder, DAGENS_DATO);

        // Act
        var resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isTrue();
    }

    /**
     * I original behandling er det utbetalt full refusjon til AG med REF3 I
     * Revurdering er det utbetalt full refusjon til samme AG men med REF1 og REF2
     * istedet Skal da opprette AP da vi ikke er sikre på hvordan vi skal
     * sammenligne andelene.
     */
    @Test
    void revurderingAndelerMedReferanseSomIkkeMatcherOriginaleAndelerOgOriginalAndelerHarIkkeNullReferanse() {
        // Arrange
        var originalBR = BeregningsresultatEntitet.builder()
                .medRegelSporing("regelsporing")
                .medRegelInput("regelinput")
                .build();

        var originalPeriode1 = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
                .build(originalBR);
        lagAndel(originalPeriode1, ARBEIDSGIVER1, true, 0, REF3);
        lagAndel(originalPeriode1, ARBEIDSGIVER1, false, 1000, REF3);
        var originalPeriode2 = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
                .build(originalBR);
        lagAndel(originalPeriode1, ARBEIDSGIVER1, true, 0, REF3);
        lagAndel(originalPeriode2, ARBEIDSGIVER1, false, 1000, REF3);

        var revurderingBR = BeregningsresultatEntitet.builder()
                .medRegelSporing("regelsporing")
                .medRegelInput("regelinput")
                .build();

        var revurderingPeriode1 = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
                .build(revurderingBR);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER1, true, 0, REF1);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER1, false, 500, REF1);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER1, true, 0, REF2);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER1, false, 500, REF2);
        var revurderingPeriode2 = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
                .build(revurderingBR);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, true, 0, REF1);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, false, 500, REF1);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, true, 0, REF2);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, false, 500, REF2);

        var forrigeTYPerioder = originalBR.getBeregningsresultatPerioder();
        var denneTYPerioder = revurderingBR.getBeregningsresultatPerioder();
        var brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinjeTest(
                forrigeTYPerioder,
                denneTYPerioder, DAGENS_DATO);

        // Act
        var resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isTrue();
    }

    private BeregningsresultatEntitet lagBeregningsresultatFP(int dagsatsBruker, int dagsatsArbeidsgiver) {
        var brpList = lagBeregningsresultatPeriode();
        brpList.forEach(brp -> {
            lagAndel(brp, ARBEIDSGIVER1, true, dagsatsBruker);
            if (dagsatsArbeidsgiver > 0) {
                lagAndel(brp, ARBEIDSGIVER1, false, dagsatsArbeidsgiver);
            }
        });
        return brpList.get(0).getBeregningsresultat();
    }

    private List<BeregningsresultatPeriode> lagBeregningsresultatPeriode() {
        var br = BeregningsresultatEntitet.builder()
                .medRegelSporing("regelsporing")
                .medRegelInput("regelinput")
                .build();

        var periode1 = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
                .build(br);
        var periode2 = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
                .build(br);
        return List.of(periode1, periode2);
    }

    private BeregningsresultatAndel lagAndel(BeregningsresultatPeriode brp, Arbeidsgiver arbeidsgiver, boolean brukerErMottaker, int dagsats) {
        return lagAndel(brp, arbeidsgiver, brukerErMottaker, dagsats, null);
    }

    private BeregningsresultatAndel lagAndel(BeregningsresultatPeriode brp, Arbeidsgiver arbeidsgiver, boolean brukerErMottaker, int dagsats,
            InternArbeidsforholdRef internRef) {
        return BeregningsresultatAndel.builder()
                .medBrukerErMottaker(brukerErMottaker)
                .medArbeidsgiver(arbeidsgiver)
                .medStillingsprosent(new BigDecimal(100))
                .medUtbetalingsgrad(new BigDecimal(100))
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medArbeidsforholdRef(internRef)
                .medDagsats(dagsats)
                .medDagsatsFraBg(dagsats)
                .build(brp);
    }

}
