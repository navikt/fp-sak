package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class OmfordelRevurderingsandelerMedReferanserTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private static final LocalDate BEREGNINGSRESULTAT_PERIODE_TOM = SKJÆRINGSTIDSPUNKT.plusDays(33);
    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.virksomhet("900050001");
    private static final InternArbeidsforholdRef REF1 = InternArbeidsforholdRef.nyRef();
    private BeregningsresultatPeriode bgBrPeriode;

    @BeforeEach
    public void setup() {
        bgBrPeriode = lagBeregningsresultatPeriode();
    }

    @Test
    public void skal_teste_at_ingenting_fordeles_ved_ingen_endring() {
        // Arrange
        List<BeregningsresultatAndel> originaleAndeler = List.of(
                lagAndel(true, 1500, REF1),
                lagAndel(false, 200, REF1));
        List<BeregningsresultatAndel> revurderingAndeler = List.of(
                lagAndel(true, 1500, REF1),
                lagAndel(false, 200, REF1));

        // Act
        List<BRNøkkelMedAndeler> originalNøkkelMedAndeler = MapAndelerSortertPåNøkkel.map(originaleAndeler);
        List<BRNøkkelMedAndeler> revurderingNøkkelAndeler = MapAndelerSortertPåNøkkel.map(revurderingAndeler);

        // Act
        var resultat = OmfordelRevurderingsandelerMedReferanser.omfordel(revurderingNøkkelAndeler.get(0), originalNøkkelMedAndeler.get(0));

        // Assert
        assertThat(resultat).hasSize(0);
    }

    @Test
    public void skal_teste_at_ingenting_fordeles_når_brukers_andel_øker() {
        // Arrange
        List<BeregningsresultatAndel> originaleAndeler = List.of(
                lagAndel(true, 1500, REF1),
                lagAndel(false, 200, REF1));
        List<BeregningsresultatAndel> revurderingAndeler = List.of(
                lagAndel(true, 1700, REF1),
                lagAndel(false, 500, REF1));

        List<BRNøkkelMedAndeler> originalNøkkelMedAndeler = MapAndelerSortertPåNøkkel.map(originaleAndeler);
        List<BRNøkkelMedAndeler> revurderingNøkkelAndeler = MapAndelerSortertPåNøkkel.map(revurderingAndeler);

        // Act
        var resultat = OmfordelRevurderingsandelerMedReferanser.omfordel(revurderingNøkkelAndeler.get(0), originalNøkkelMedAndeler.get(0));

        // Assert
        assertThat(resultat).hasSize(0);
    }

    @Test
    public void skal_teste_at_refordeling_skjer_når_bruker_mister_penger() {
        // Arrange
        List<BeregningsresultatAndel> originaleAndeler = List.of(
                lagAndel(true, 1500, REF1),
                lagAndel(false, 200, REF1));
        List<BeregningsresultatAndel> revurderingAndeler = List.of(
                lagAndel(true, 400, REF1),
                lagAndel(false, 1300, REF1));

        List<BRNøkkelMedAndeler> originalNøkkelMedAndeler = MapAndelerSortertPåNøkkel.map(originaleAndeler);
        List<BRNøkkelMedAndeler> revurderingNøkkelAndeler = MapAndelerSortertPåNøkkel.map(revurderingAndeler);

        // Act
        var resultat = OmfordelRevurderingsandelerMedReferanser.omfordel(revurderingNøkkelAndeler.get(0), originalNøkkelMedAndeler.get(0));

        // Assert
        assertThat(resultat).hasSize(2);
    }

    private BeregningsresultatPeriode lagBeregningsresultatPeriode() {
        BeregningsresultatEntitet br = BeregningsresultatEntitet.builder()
                .medRegelInput("input")
                .medRegelSporing("sporing")
                .build();
        return BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, BEREGNINGSRESULTAT_PERIODE_TOM)
                .build(br);
    }

    private BeregningsresultatAndel lagAndel(boolean erBrukerMottaker, int dagsats, InternArbeidsforholdRef ref) {
        return BeregningsresultatAndel.builder()
                .medBrukerErMottaker(erBrukerMottaker)
                .medStillingsprosent(BigDecimal.valueOf(100))
                .medUtbetalingsgrad(BigDecimal.valueOf(100))
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medDagsatsFraBg(dagsats)
                .medDagsats(dagsats)
                .medArbeidsgiver(ARBEIDSGIVER)
                .medArbeidsforholdRef(ref)
                .build(bgBrPeriode);
    }
}
