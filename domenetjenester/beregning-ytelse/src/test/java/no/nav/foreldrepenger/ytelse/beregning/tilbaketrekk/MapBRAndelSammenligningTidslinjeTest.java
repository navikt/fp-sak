package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
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
import no.nav.fpsak.tidsserie.LocalDateSegment;

class MapBRAndelSammenligningTidslinjeTest {

    private static final LocalDate STP = LocalDate.of(2019, 9, 1);
    private static final Arbeidsgiver AG1 = Arbeidsgiver.virksomhet("999999999");
    private static final Arbeidsgiver AG2 = Arbeidsgiver.virksomhet("888888888");
    private static final InternArbeidsforholdRef REF1 = InternArbeidsforholdRef.nyRef();
    private static final InternArbeidsforholdRef REF2 = InternArbeidsforholdRef.nyRef();

    @Test
    void skal_teste_at_tidslinje_lages_korrekt_når_begge_resultat_er_like_og_ingenting_er_utbetalt() {
        // Arrange
        var dagensdato = STP.minusDays(1);
        var periode = lagResultatMedPeriode(STP, STP.plusDays(15));
        var periode2 = lagResultatMedPeriode(STP.plusDays(16), STP.plusDays(29));
        var periode3 = lagResultatMedPeriode(STP.plusDays(30), STP.plusDays(40));
        var andel = lagAndelForPeriode(periode, AG1, REF1);
        lagAndelForPeriode(periode2, AG1, REF1);
        lagAndelForPeriode(periode3, AG1, REF1);

        // Act
        var tidslinje = MapBRAndelSammenligningTidslinje
                .opprettTidslinjeTest(Arrays.asList(periode, periode2, periode3), Arrays.asList(periode, periode2, periode3), dagensdato);

        // Assert
        assertThat(tidslinje.toSegments()).hasSize(3);
        var segmenter = new ArrayList<LocalDateSegment<BRAndelSammenligning>>(tidslinje.toSegments());

        assertSegment(segmenter.get(0), STP, STP.plusDays(15), Collections.singletonList(andel), Collections.emptyList());
        assertSegment(segmenter.get(1), STP.plusDays(16), STP.plusDays(29), Collections.singletonList(andel), Collections.emptyList());
        assertSegment(segmenter.get(2), STP.plusDays(30), STP.plusDays(40), Collections.singletonList(andel), Collections.emptyList());
    }

    @Test
    void skal_teste_at_tidslinje_lages_korrekt_når_begge_resultat_er_like_og_noe_er_utbetalt() {
        // Arrange
        var dagensdato = STP.plusDays(20);
        var periode = lagResultatMedPeriode(STP, STP.plusDays(15));
        var periode2 = lagResultatMedPeriode(STP.plusDays(16), STP.plusDays(25));
        var periode3 = lagResultatMedPeriode(STP.plusDays(26), STP.plusDays(40));
        var andel = lagAndelForPeriode(periode, AG1, REF1);
        lagAndelForPeriode(periode2, AG1, REF1);
        lagAndelForPeriode(periode3, AG1, REF1);

        // Act
        var tidslinje = MapBRAndelSammenligningTidslinje
                .opprettTidslinjeTest(Arrays.asList(periode, periode2, periode3), Arrays.asList(periode, periode2, periode3), dagensdato);

        // Assert
        assertThat(tidslinje.toSegments()).hasSize(4);
        var segmenter = new ArrayList<LocalDateSegment<BRAndelSammenligning>>(tidslinje.toSegments());

        assertSegment(segmenter.get(0), STP, STP.plusDays(15), Collections.singletonList(andel), Collections.singletonList(andel));
        assertSegment(segmenter.get(1), STP.plusDays(16), STP.plusDays(25), Collections.singletonList(andel), Collections.singletonList(andel));
        assertSegment(segmenter.get(2), STP.plusDays(26), STP.plusDays(29), Collections.singletonList(andel), Collections.singletonList(andel));
        assertSegment(segmenter.get(3), STP.plusDays(30), STP.plusDays(40), Collections.singletonList(andel), Collections.emptyList());
    }

    @Test
    void skal_teste_at_tidslinje_lages_korrekt_når_nytt_resultat_har_ekstra_andel_og_noe_er_utbetalt() {
        // Arrange
        var dagensdato = STP.plusDays(20);

        // Gammelt resultat
        var gammelPeriode = lagResultatMedPeriode(STP, STP.plusDays(15));
        var gammelPeriode2 = lagResultatMedPeriode(STP.plusDays(16), STP.plusDays(25));
        var gammelPeriode3 = lagResultatMedPeriode(STP.plusDays(26), STP.plusDays(40));
        var gammelAndel = lagAndelForPeriode(gammelPeriode, AG1, REF1);
        lagAndelForPeriode(gammelPeriode2, AG1, REF1);
        lagAndelForPeriode(gammelPeriode3, AG1, REF1);

        // Nytt resultat
        var nyPeriode = lagResultatMedPeriode(STP, STP.plusDays(15));
        var nyPeriode2 = lagResultatMedPeriode(STP.plusDays(16), STP.plusDays(25));
        var nyPeriode3 = lagResultatMedPeriode(STP.plusDays(26), STP.plusDays(40));
        var nyAndel = lagAndelForPeriode(nyPeriode, AG1, REF1);
        lagAndelForPeriode(nyPeriode2, AG1, REF1);
        var nyAndel2 = lagAndelForPeriode(nyPeriode2, AG2, REF2);
        lagAndelForPeriode(nyPeriode3, AG1, REF1);

        // Act
        var tidslinje = MapBRAndelSammenligningTidslinje
                .opprettTidslinjeTest(Arrays.asList(gammelPeriode, gammelPeriode2, gammelPeriode3), Arrays.asList(nyPeriode, nyPeriode2, nyPeriode3),
                        dagensdato);

        // Assert
        assertThat(tidslinje.toSegments()).hasSize(4);
        var segmenter = new ArrayList<LocalDateSegment<BRAndelSammenligning>>(tidslinje.toSegments());

        assertSegment(segmenter.get(0), STP, STP.plusDays(15), Collections.singletonList(nyAndel), Collections.singletonList(gammelAndel));
        assertSegment(segmenter.get(1), STP.plusDays(16), STP.plusDays(25), Arrays.asList(nyAndel, nyAndel2), Collections.singletonList(gammelAndel));
        assertSegment(segmenter.get(2), STP.plusDays(26), STP.plusDays(29), Collections.singletonList(nyAndel),
                Collections.singletonList(gammelAndel));
        assertSegment(segmenter.get(3), STP.plusDays(30), STP.plusDays(40), Collections.singletonList(nyAndel), Collections.emptyList());
    }

    private void assertSegment(LocalDateSegment<BRAndelSammenligning> segment, LocalDate fom, LocalDate tom,
            List<BeregningsresultatAndel> nyeForventedeAndeler, List<BeregningsresultatAndel> forrigeForventedeAndeler) {
        assertThat(segment.getFom()).isEqualTo(fom);
        assertThat(segment.getTom()).isEqualTo(tom);

        var nyeAndeler = segment.getValue().getBgAndeler();
        var forrigeAndeler = segment.getValue().getForrigeAndeler();

        // Listene skal inneholde de samme elementene (rekkefølge er ikke viktig)
        assertThat(nyeAndeler).containsExactlyInAnyOrderElementsOf(nyeForventedeAndeler);
        assertThat(forrigeAndeler).containsExactlyInAnyOrderElementsOf(forrigeForventedeAndeler);
    }

    private BeregningsresultatAndel lagAndelForPeriode(BeregningsresultatPeriode periode, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref) {
        return BeregningsresultatAndel.builder()
                .medBrukerErMottaker(false)
                .medStillingsprosent(BigDecimal.valueOf(100))
                .medUtbetalingsgrad(BigDecimal.valueOf(100))
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medDagsats(900)
                .medDagsatsFraBg(900)
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdRef(ref)
                .build(periode);
    }

    private BeregningsresultatPeriode lagResultatMedPeriode(LocalDate fom, LocalDate tom) {
        var resultat = BeregningsresultatEntitet.builder().medRegelInput("test").medRegelSporing("test").build();
        return BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(fom, tom)
                .build(resultat);
    }

}
