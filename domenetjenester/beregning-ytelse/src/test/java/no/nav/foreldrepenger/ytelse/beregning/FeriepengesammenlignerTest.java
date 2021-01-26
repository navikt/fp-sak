package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FeriepengesammenlignerTest {
    private BeregningsresultatEntitet gr1;
    private BeregningsresultatEntitet gr2;
    private Feriepengesammenligner sjekker;

    @BeforeEach
    void oppsett() {
        gr2 = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        gr1 = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.now(), LocalDate.now().plusMonths(1))
            .build(gr2);
        BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.now(), LocalDate.now().plusMonths(1))
            .build(gr1);
        BeregningsresultatFeriepenger.builder().medFeriepengerRegelSporing("").medFeriepengerRegelInput("").medFeriepengerPeriodeFom(LocalDate.now()).medFeriepengerPeriodeFom(LocalDate.now()).build(gr2);
        BeregningsresultatFeriepenger.builder().medFeriepengerRegelSporing("").medFeriepengerRegelInput("").medFeriepengerPeriodeFom(LocalDate.now()).medFeriepengerPeriodeFom(LocalDate.now()).build(gr1);
        sjekker = new Feriepengesammenligner(123, gr1, gr2);
    }

    @Test
    public void skal_teste_ingen_avvik() {
        // Arrange
        lagFerieandel(gr1, 2020, 5000, lagAndel(gr1, "123", true));
        lagFerieandel(gr1, 2020, 5000, lagAndel(gr1, "123", false));

        lagFerieandel(gr2, 2020, 5000, lagAndel(gr2, "123", true));
        lagFerieandel(gr2, 2020, 5000, lagAndel(gr2, "123", false));

        // Act
        boolean finnesAvvik = sjekker.finnesAvvik();

        // Assert
        assertThat(finnesAvvik).isFalse();
    }

    @Test
    public void skal_gi_avvik_når_beløp_er_ulikt() {
        // Arrange
        lagFerieandel(gr1, 2020, 4999, lagAndel(gr1, "123", true));
        lagFerieandel(gr1, 2020, 5000, lagAndel(gr1, "123", false));

        lagFerieandel(gr2, 2020, 5000, lagAndel(gr2, "123", true));
        lagFerieandel(gr2, 2020, 5000, lagAndel(gr2, "123", false));

        // Act
        boolean finnesAvvik = sjekker.finnesAvvik();

        // Assert
        assertThat(finnesAvvik).isTrue();
    }

    @Test
    public void skal_gi_avvik_når_nøkkel_er_ulik() {
        // Arrange
        lagFerieandel(gr1, 2020, 5000, lagAndel(gr1, "123", true));
        lagFerieandel(gr1, 2020, 5000, lagAndel(gr1, "123", false));

        lagFerieandel(gr2, 2020, 5000, lagAndel(gr2, "321", true));
        lagFerieandel(gr2, 2020, 5000, lagAndel(gr2, "321", false));

        // Act
        boolean finnesAvvik = sjekker.finnesAvvik();

        // Assert
        assertThat(finnesAvvik).isTrue();
    }

    @Test
    public void skal_gi_avvik_når_et_av_flere_år_er_ulikt() {
        // Arrange
        BeregningsresultatAndel andel1 = lagAndel(gr1, "123", true);
        lagFerieandel(gr1, 2020, 5000, andel1);
        lagFerieandel(gr1, 2020, 5000, lagAndel(gr1, "321", true));
        lagFerieandel(gr1, 2021, 5000, andel1);

        BeregningsresultatAndel andel2 = lagAndel(gr2, "123", true);
        lagFerieandel(gr2, 2020, 5000, andel2);
        lagFerieandel(gr2, 2020, 5000, lagAndel(gr2, "321", true));
        lagFerieandel(gr2, 2021, 4999, andel2);

        // Act
        boolean finnesAvvik = sjekker.finnesAvvik();

        // Assert
        assertThat(finnesAvvik).isTrue();
    }

    private void lagFerieandel(BeregningsresultatEntitet gr, int år, int beløp, BeregningsresultatAndel andel) {
        BeregningsresultatFeriepengerPrÅr.builder().medÅrsbeløp(beløp).medOpptjeningsår(år).build(gr.getBeregningsresultatFeriepenger().get(), andel);
    }

    private BeregningsresultatAndel lagAndel(BeregningsresultatEntitet gr, String orgnr, boolean brukerErMottaker) {
        return BeregningsresultatAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .medDagsats(500)
            .medDagsatsFraBg(500)
            .medBrukerErMottaker(brukerErMottaker)
            .medStillingsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .build(gr.getBeregningsresultatPerioder().get(0));
    }

}
