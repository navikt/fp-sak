package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.vedtak.util.FPDateUtil;

public class VurderBehovForÅHindreTilbaketrekkTest {
    private static final String FUNKSJONELT_TIDSOFFSET = FPDateUtil.SystemConfiguredClockProvider.PROPERTY_KEY_OFFSET_PERIODE;

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019, Month.JANUARY, 20);
    private static final LocalDate ANDRE_PERIODE_FOM = SKJÆRINGSTIDSPUNKT.plusMonths(5);
    private static final LocalDate SISTE_UTTAKSDAG = SKJÆRINGSTIDSPUNKT.plusMonths(9);

    private static final Arbeidsgiver ARBEIDSGIVER1 = Arbeidsgiver.virksomhet("900050001");
    private static final Arbeidsgiver ARBEIDSGIVER2 = Arbeidsgiver.virksomhet(KUNSTIG_ORG);

    private static final InternArbeidsforholdRef REF1 = InternArbeidsforholdRef.nyRef();
    private static final InternArbeidsforholdRef REF2 = InternArbeidsforholdRef.nyRef();
    private static final InternArbeidsforholdRef REF3 = InternArbeidsforholdRef.nyRef();

    @BeforeAll
    public static void beforeClass() {
        settSimulertNåtidTil(LocalDate.of(2019, Month.FEBRUARY, 4));
    }

    @AfterAll
    public static void teardown() {
        settSimulertNåtidTil(LocalDate.now());
        FPDateUtil.init();
    }

    @Test
    public void ingenEndringSkalGiEmpty() {
        // Arrange
        int inntektBeløp = 1000;
        BeregningsresultatEntitet forrigeTY = lagBeregningsresultatFP(0, inntektBeløp);
        BeregningsresultatEntitet denneTY = lagBeregningsresultatFP(0, inntektBeløp);
        List<BeregningsresultatPeriode> forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> denneTYPerioder = denneTY.getBeregningsresultatPerioder();
        LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTYPerioder,
            denneTYPerioder
        );

        // Act
        boolean resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void økningIRefusjonOgReduksjonFraBrukerSkalGiEndringsdato() {
        // Arrange
        BeregningsresultatEntitet forrigeTY = lagBeregningsresultatFP(200, 800);
        BeregningsresultatEntitet denneTY = lagBeregningsresultatFP(0, 1000);
        List<BeregningsresultatPeriode> forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> denneTYPerioder = denneTY.getBeregningsresultatPerioder();

        // Act
        boolean resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTYPerioder,
            denneTYPerioder
        ), List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void ingenRefusjonIForrigeOgFullRefusjonIRevurderingSkalGiEndringsdato() {
        // Arrange
        BeregningsresultatEntitet forrigeTY = lagBeregningsresultatFP(1000, 0);
        BeregningsresultatEntitet denneTY = lagBeregningsresultatFP(0, 1000);
        List<BeregningsresultatPeriode> forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> denneTYPerioder = denneTY.getBeregningsresultatPerioder();
        LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTYPerioder,
            denneTYPerioder
        );

        // Act
        boolean resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void ingenTilkjentYtelseIRevurderingSkalGiEmpty() {
        // Arrange
        BeregningsresultatEntitet forrigeTY = lagBeregningsresultatFP(0, 1000);
        List<BeregningsresultatPeriode> forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> denneTYPerioder = Collections.emptyList();
        LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTYPerioder,
            denneTYPerioder
        );

        // Act
        boolean resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void økningSkalGiEmpty() {
        // Arrange
        BeregningsresultatEntitet forrigeTY = lagBeregningsresultatFP(0, 1000);
        BeregningsresultatEntitet denneTY = lagBeregningsresultatFP(200, 800);
        List<BeregningsresultatPeriode> forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> denneTYPerioder = denneTY.getBeregningsresultatPerioder();
        LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTYPerioder,
            denneTYPerioder
        );

        // Act
        boolean resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void reduksjonEtterUtbetaltTomSkalGiEmpty() {
        // Arrange
        BeregningsresultatEntitet forrigeTY = lagBeregningsresultatFP(200, 800);

        BeregningsresultatEntitet br = BeregningsresultatEntitet.builder()
            .medRegelSporing("regelsporing")
            .medRegelInput("regelinput")
            .build();

        BeregningsresultatPeriode periode1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
            .build(br);
        lagAndel(periode1, ARBEIDSGIVER1, true, 200);
        lagAndel(periode1, ARBEIDSGIVER1, false, 800);
        BeregningsresultatPeriode periode2 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
            .build(br);
        lagAndel(periode2, ARBEIDSGIVER1, true, 0);
        lagAndel(periode2, ARBEIDSGIVER1, false, 1000);
        BeregningsresultatEntitet denneTY = periode1.getBeregningsresultat();

        List<BeregningsresultatPeriode> forrigeTYPerioder = forrigeTY.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> denneTYPerioder = denneTY.getBeregningsresultatPerioder();
        LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTYPerioder,
            denneTYPerioder
        );

        // Act
        boolean resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void bortfaltAndelSkalIkkeGiAksjonspunkt() {
        // Arrange
        BeregningsresultatEntitet originalBR = BeregningsresultatEntitet.builder()
            .medRegelSporing("regelsporing")
            .medRegelInput("regelinput")
            .build();

        BeregningsresultatPeriode originalPeriode1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
            .build(originalBR);
        lagAndel(originalPeriode1, ARBEIDSGIVER1, true, 200);
        lagAndel(originalPeriode1, ARBEIDSGIVER1, false, 800);
        BeregningsresultatPeriode originalPeriode2 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
            .build(originalBR);
        lagAndel(originalPeriode2, ARBEIDSGIVER1, true, 0);
        lagAndel(originalPeriode2, ARBEIDSGIVER1, false, 1000);

        BeregningsresultatEntitet revurderingBR = BeregningsresultatEntitet.builder()
            .medRegelSporing("regelsporing")
            .medRegelInput("regelinput")
            .build();

        BeregningsresultatPeriode revurderingPeriode1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
            .build(revurderingBR);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER2, true, 200);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER2, false, 800);
        BeregningsresultatPeriode revurderingPeriode2 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
            .build(revurderingBR);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, true, 0);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, false, 1000);

        List<BeregningsresultatPeriode> forrigeTYPerioder = originalBR.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> denneTYPerioder = revurderingBR.getBeregningsresultatPerioder();
        LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTYPerioder,
            denneTYPerioder
        );

        // Act
        boolean resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void originalAndelUtenReferanseOgRefusjonSplittetTilToAndelerMedReferanseOgRefusjon() {
        // Arrange
        BeregningsresultatEntitet originalBR = BeregningsresultatEntitet.builder()
            .medRegelSporing("regelsporing")
            .medRegelInput("regelinput")
            .build();

        BeregningsresultatPeriode originalPeriode1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
            .build(originalBR);
        lagAndel(originalPeriode1, ARBEIDSGIVER1, true, 1000, null);
        BeregningsresultatPeriode originalPeriode2 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
            .build(originalBR);
        lagAndel(originalPeriode2, ARBEIDSGIVER1, true, 1000, null);

        BeregningsresultatEntitet revurderingBR = BeregningsresultatEntitet.builder()
            .medRegelSporing("regelsporing")
            .medRegelInput("regelinput")
            .build();

        BeregningsresultatPeriode revurderingPeriode1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
            .build(revurderingBR);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER1, true, 0, REF1);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER1, false, 500, REF1);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER1, true, 0, REF2);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER1, false, 500, REF2);
        BeregningsresultatPeriode revurderingPeriode2 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
            .build(revurderingBR);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, true, 0, REF1);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, false, 500, REF1);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, true, 0, REF2);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, false, 500, REF2);

        List<BeregningsresultatPeriode> forrigeTYPerioder = originalBR.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> denneTYPerioder = revurderingBR.getBeregningsresultatPerioder();
        LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTYPerioder,
            denneTYPerioder
        );

        // Act
        boolean resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isTrue();
    }

    /**
     * I original behandling er det utbetalt full refusjon til AG med REF3
     * I Revurdering er det utbetalt full refusjon til samme AG men med REF1 og REF2 istedet
     * Skal da opprette AP da vi ikke er sikre på hvordan vi skal sammenligne andelene.
     */
    @Test
    public void revurderingAndelerMedReferanseSomIkkeMatcherOriginaleAndelerOgOriginalAndelerHarIkkeNullReferanse() {
        // Arrange
        BeregningsresultatEntitet originalBR = BeregningsresultatEntitet.builder()
            .medRegelSporing("regelsporing")
            .medRegelInput("regelinput")
            .build();

        BeregningsresultatPeriode originalPeriode1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
            .build(originalBR);
        lagAndel(originalPeriode1, ARBEIDSGIVER1, true, 0, REF3);
        lagAndel(originalPeriode1, ARBEIDSGIVER1, false, 1000, REF3);
        BeregningsresultatPeriode originalPeriode2 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
            .build(originalBR);
        lagAndel(originalPeriode1, ARBEIDSGIVER1, true, 0, REF3);
        lagAndel(originalPeriode2, ARBEIDSGIVER1, false, 1000, REF3);

        BeregningsresultatEntitet revurderingBR = BeregningsresultatEntitet.builder()
            .medRegelSporing("regelsporing")
            .medRegelInput("regelinput")
            .build();

        BeregningsresultatPeriode revurderingPeriode1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
            .build(revurderingBR);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER1, true, 0, REF1);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER1, false, 500, REF1);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER1, true, 0, REF2);
        lagAndel(revurderingPeriode1, ARBEIDSGIVER1, false, 500, REF2);
        BeregningsresultatPeriode revurderingPeriode2 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
            .build(revurderingBR);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, true, 0, REF1);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, false, 500, REF1);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, true, 0, REF2);
        lagAndel(revurderingPeriode2, ARBEIDSGIVER1, false, 500, REF2);

        List<BeregningsresultatPeriode> forrigeTYPerioder = originalBR.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> denneTYPerioder = revurderingBR.getBeregningsresultatPerioder();
        LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje = MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTYPerioder,
            denneTYPerioder
        );

        // Act
        boolean resultat = VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje, List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(resultat).isTrue();
    }


    private BeregningsresultatEntitet lagBeregningsresultatFP(int dagsatsBruker, int dagsatsArbeidsgiver) {
        List<BeregningsresultatPeriode> brpList = lagBeregningsresultatPeriode();
        brpList.forEach(brp -> {
            lagAndel(brp, ARBEIDSGIVER1, true, dagsatsBruker);
            if (dagsatsArbeidsgiver > 0) {
                lagAndel(brp, ARBEIDSGIVER1, false, dagsatsArbeidsgiver);
            }
        });
        return brpList.get(0).getBeregningsresultat();
    }

    private List<BeregningsresultatPeriode> lagBeregningsresultatPeriode() {
        BeregningsresultatEntitet br = BeregningsresultatEntitet.builder()
            .medRegelSporing("regelsporing")
            .medRegelInput("regelinput")
            .build();

        BeregningsresultatPeriode periode1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, ANDRE_PERIODE_FOM.minusDays(1))
            .build(br);
        BeregningsresultatPeriode periode2 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(ANDRE_PERIODE_FOM, SISTE_UTTAKSDAG)
            .build(br);
        return List.of(periode1, periode2);
    }

    private BeregningsresultatAndel lagAndel(BeregningsresultatPeriode brp, Arbeidsgiver arbeidsgiver, boolean brukerErMottaker, int dagsats) {
        return lagAndel(brp, arbeidsgiver, brukerErMottaker, dagsats, null);
    }

    private BeregningsresultatAndel lagAndel(BeregningsresultatPeriode brp, Arbeidsgiver arbeidsgiver, boolean brukerErMottaker, int dagsats, InternArbeidsforholdRef internRef) {
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

    private static void settSimulertNåtidTil(LocalDate dato) {
        Period periode = Period.between(LocalDate.now(), dato);
        System.setProperty(FUNKSJONELT_TIDSOFFSET, periode.toString());
        FPDateUtil.init();
    }
}
