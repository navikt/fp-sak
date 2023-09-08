package no.nav.foreldrepenger.behandling.steg.beregnytelse;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

import no.nav.foreldrepenger.ytelse.beregning.Virkedager;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EtterbetalingtjenesteTest {
    private BeregningsresultatEntitet originaltResultat = BeregningsresultatEntitet.builder().medRegelInput("").medRegelSporing("").build();
    private BeregningsresultatEntitet nyttResultat = BeregningsresultatEntitet.builder().medRegelInput("").medRegelSporing("").build();

    @Test
    void skal_teste_når_ingen_etterbetaling() {
        lagOriginalPeriode(LocalDate.of(2023,1,1), LocalDate.of(2023,3,1), 500, true);
        lagOriginalPeriode(LocalDate.of(2023,3,2), LocalDate.of(2023,5,31), 200, true);

        lagNyPeriode(LocalDate.of(2023,1,1), LocalDate.of(2023,3,1), 500, true);
        lagNyPeriode(LocalDate.of(2023,3,2), LocalDate.of(2023,5,31), 200, true);

        var etterbetalingsKontroll = Etterbetalingtjeneste.finnSumSomVilBliEtterbetalt(LocalDate.of(2023, 6, 1), originaltResultat, nyttResultat);
        assertThat(etterbetalingsKontroll.overstigerGrense()).isFalse();
        assertThat(etterbetalingsKontroll.etterbetalingssum()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void skal_kun_telle_direkteutbetaling() {
        lagOriginalPeriode(LocalDate.of(2023,1,1), LocalDate.of(2023,3,1), 500, true);
        lagOriginalPeriode(LocalDate.of(2023,3,2), LocalDate.of(2023,5,31), 200, false);

        lagNyPeriode(LocalDate.of(2023,1,1), LocalDate.of(2023,3,1), 500, true);
        lagNyPeriode(LocalDate.of(2023,3,2), LocalDate.of(2023,5,31), 300, false);

        var etterbetalingsKontroll = Etterbetalingtjeneste.finnSumSomVilBliEtterbetalt(LocalDate.of(2023, 6, 1), originaltResultat, nyttResultat);
        assertThat(etterbetalingsKontroll.overstigerGrense()).isFalse();
        assertThat(etterbetalingsKontroll.etterbetalingssum()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void skal_teste_når_etterbetaling_ikke_over_grense() {
        lagOriginalPeriode(LocalDate.of(2023,1,1), LocalDate.of(2023,3,1), 500, true);
        lagOriginalPeriode(LocalDate.of(2023,3,2), LocalDate.of(2023,5,31), 200, true);

        lagNyPeriode(LocalDate.of(2023,1,1), LocalDate.of(2023,3,1), 500, true);
        lagNyPeriode(LocalDate.of(2023,3,2), LocalDate.of(2023,5,31), 250, true);

        var etterbetalingsKontroll = Etterbetalingtjeneste.finnSumSomVilBliEtterbetalt(LocalDate.of(2023, 6, 1), originaltResultat, nyttResultat);

        var virkedager = Virkedager.beregnAntallVirkedager(LocalDate.of(2023, 3, 2), LocalDate.of(2023, 5, 31));
        assertThat(etterbetalingsKontroll.overstigerGrense()).isFalse();
        assertThat(etterbetalingsKontroll.etterbetalingssum()).isEqualByComparingTo(BigDecimal.valueOf(50L*virkedager));
    }

    @Test
    void skal_teste_når_etterbetaling_over_grense() {
        lagOriginalPeriode(LocalDate.of(2023,1,1), LocalDate.of(2023,3,1), 2350, false);
        lagOriginalPeriode(LocalDate.of(2023,3,2), LocalDate.of(2023,9,30), 2350, false);

        lagNyPeriode(LocalDate.of(2023,1,1), LocalDate.of(2023,3,1), 2350, true);
        lagNyPeriode(LocalDate.of(2023,3,2), LocalDate.of(2023,5,31), 2350, true);
        lagNyPeriode(LocalDate.of(2023,6,1), LocalDate.of(2023,9,30), 2350, true);

        var etterbetalingsKontroll = Etterbetalingtjeneste.finnSumSomVilBliEtterbetalt(LocalDate.of(2023, 10, 1), originaltResultat, nyttResultat);

        var virkedager = Virkedager.beregnAntallVirkedager(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 9, 30));
        assertThat(etterbetalingsKontroll.overstigerGrense()).isTrue();
        assertThat(etterbetalingsKontroll.etterbetalingssum()).isEqualByComparingTo(BigDecimal.valueOf(2350L*virkedager));
    }

    private void lagOriginalPeriode(LocalDate fom, LocalDate tom, int dagsats, boolean brukerErMottaker) {
        var periode = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(fom, tom).build(originaltResultat);
        lagAndel(dagsats, brukerErMottaker, periode);
    }

    private void lagAndel(int dagsats, boolean brukerErMottaker, BeregningsresultatPeriode periode) {
        BeregningsresultatAndel.builder()
            .medDagsats(dagsats)
            .medBrukerErMottaker(brukerErMottaker)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999"))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsatsFraBg(dagsats)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .build(periode);
    }

    private void lagNyPeriode(LocalDate fom, LocalDate tom, int dagsats, boolean brukerErMottaker) {
        var periode = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(fom, tom).build(nyttResultat);
        lagAndel(dagsats, brukerErMottaker, periode);
    }

}
