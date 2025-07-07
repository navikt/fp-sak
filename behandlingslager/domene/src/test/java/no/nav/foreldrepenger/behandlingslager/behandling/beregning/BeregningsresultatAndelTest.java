package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

class BeregningsresultatAndelTest {

    private static final String ORGNR = "6543795";

    private BeregningsresultatPeriode periode;

    @BeforeEach
    void oppsett() {
        var beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("clob1").medRegelSporing("clob2").build();
        periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.of(2018, 8, 22), LocalDate.of(2018, 9, 22))
            .build(beregningsresultat);
    }

    @Test
    void andel_hvor_alle_feltene_er_satt_uten_arbeidsgiver() {
        var andel = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(80))
            .medDagsatsFraBg(450)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medArbeidsgiver(null)
            .build(periode);
        assertThat(andel.erBrukerMottaker()).isTrue();
        assertThat(andel.getDagsats()).isEqualTo(550);
        assertThat(andel.getStillingsprosent()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(andel.getUtbetalingsgrad()).isEqualTo(BigDecimal.valueOf(80));
        assertThat(andel.getDagsatsFraBg()).isEqualTo(450);
        assertThat(andel.getInntektskategori()).isEqualTo(Inntektskategori.ARBEIDSTAKER);
        assertThat(andel.getArbeidsgiver()).isNotPresent();
    }

    @Test
    void andel_hvor_alle_feltene_er_satt_med_arbeidsgiver() {
        var andel = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(false)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medDagsatsFraBg(450)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
            .build(periode);
        assertThat(andel.erBrukerMottaker()).isFalse();
        assertThat(andel.getDagsats()).isEqualTo(550);
        assertThat(andel.getStillingsprosent()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(andel.getUtbetalingsgrad()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(andel.getDagsatsFraBg()).isEqualTo(450);
        assertThat(andel.getInntektskategori()).isEqualTo(Inntektskategori.ARBEIDSTAKER);
        assertThat(andel.getArbeidsgiver().orElseThrow().getOrgnr()).isEqualTo(ORGNR);
    }

    @Test
    void andel_hvor_brukerErMottaker_er_true_og_arbeidsgiver_er_satt() {
        var andel = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(0))
            .medDagsatsFraBg(450)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
            .build(periode);
        assertThat(andel.erBrukerMottaker()).isTrue();
        assertThat(andel.getDagsats()).isEqualTo(550);
        assertThat(andel.getStillingsprosent()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(andel.getUtbetalingsgrad()).isEqualTo(BigDecimal.valueOf(0));
        assertThat(andel.getDagsatsFraBg()).isEqualTo(450);
        assertThat(andel.getInntektskategori()).isEqualTo(Inntektskategori.ARBEIDSTAKER);
        assertThat(andel.getArbeidsgiver().orElseThrow().getOrgnr()).isEqualTo(ORGNR);
    }

    @Test
    void andel_hvor_brukerErMottaker_er_false_og_arbeidsgiver_er_null() {
        var builder = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(false)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(80))
            .medDagsatsFraBg(450)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsgiver(null);
        assertThrows(NullPointerException.class, () -> builder.build(periode));
    }

    @Test
    void andel_hvor_brukerErMottaker_ikke_er_satt() {
        var builder = BeregningsresultatAndel.builder()
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medDagsatsFraBg(550)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsgiver(null);
        assertThrows(NullPointerException.class, () -> builder.build(periode));
    }

    @Test
    void andel_hvor_dagsats_ikke_er_satt() {
        var builder = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medDagsatsFraBg(550)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsgiver(null);
        assertThrows(NullPointerException.class, () -> builder.build(periode));
    }

    @Test
    void andel_hvor_stillingsprosent_ikke_er_satt() {
        var builder = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medDagsatsFraBg(550)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsgiver(null);
        assertThrows(NullPointerException.class, () -> builder.build(periode));
    }

    @Test
    void andel_hvor_utbetalingsgrad_ikke_er_satt() {
        var builder = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medDagsatsFraBg(550)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsgiver(null);
        assertThrows(NullPointerException.class, () -> builder.build(periode));
    }

    @Test
    void andel_hvor_dagsatsFraBg_ikke_er_satt() {
        var builder = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsgiver(null);
        assertThrows(NullPointerException.class, () -> builder.build(periode));
    }

    @Test
    void andel_hvor_inntektskategori_ikke_er_satt() {
        var builder = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medDagsatsFraBg(550)
            .medArbeidsgiver(null);
        assertThrows(NullPointerException.class, () -> builder.build(periode));
    }

    @Test
    void andel_hvor_utbetalingsgraden_er_minus_1() {
        var builder = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(-1))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsatsFraBg(550)
            .medArbeidsgiver(null);
        assertThrows(IllegalStateException.class, () -> builder.build(periode));
    }

    @Test
    void andel_hvor_utbetalingsgraden_er_101() {
        var builder = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(101))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsatsFraBg(550)
            .medArbeidsgiver(null);
        assertThrows(IllegalStateException.class, () -> builder.build(periode));
    }

    @Test
    void andel_hvor_inntektskategorien_er_udefinert() {
        var builder = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.UDEFINERT)
            .medAktivitetStatus(AktivitetStatus.UDEFINERT)
            .medDagsatsFraBg(550)
            .medArbeidsgiver(null);
        assertThrows(IllegalStateException.class, () -> builder.build(periode));
    }

}
