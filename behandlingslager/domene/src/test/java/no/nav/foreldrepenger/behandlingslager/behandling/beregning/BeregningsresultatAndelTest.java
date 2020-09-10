package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

public class BeregningsresultatAndelTest {

    private static final String ORGNR = "6543795";

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private BeregningsresultatPeriode periode;

    @Before
    public void oppsett() {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(
                LocalDate.of(2018, 8, 22),
                LocalDate.of(2018, 9, 22))
            .build(beregningsresultat);
    }

    @Test
    public void andel_hvor_alle_feltene_er_satt_uten_arbeidsgiver() {
        BeregningsresultatAndel andel = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(80))
            .medDagsatsFraBg(450)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
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
    public void andel_hvor_alle_feltene_er_satt_med_arbeidsgiver() {
        BeregningsresultatAndel andel = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(false)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medDagsatsFraBg(450)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
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
    public void andel_hvor_brukerErMottaker_er_true_og_arbeidsgiver_er_satt() {
        BeregningsresultatAndel andel = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(0))
            .medDagsatsFraBg(450)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
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
    public void andel_hvor_brukerErMottaker_er_false_og_arbeidsgiver_er_null() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("virksomhet");
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(false)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(80))
            .medDagsatsFraBg(450)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsgiver(null)
            .build(periode);
    }

    @Test
    public void andel_hvor_brukerErMottaker_ikke_er_satt() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("brukerErMottaker");
        BeregningsresultatAndel.builder()
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medDagsatsFraBg(550)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsgiver(null)
            .build(periode);
    }

    @Test
    public void andel_hvor_dagsats_ikke_er_satt() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("dagsats");
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medDagsatsFraBg(550)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsgiver(null)
            .build(periode);
    }

    @Test
    public void andel_hvor_stillingsprosent_ikke_er_satt() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("stillingsprosent");
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medDagsatsFraBg(550)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsgiver(null)
            .build(periode);
    }

    @Test
    public void andel_hvor_utbetalingsgrad_ikke_er_satt() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("utbetalingsgrad");
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medDagsatsFraBg(550)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsgiver(null)
            .build(periode);
    }

    @Test
    public void andel_hvor_dagsatsFraBg_ikke_er_satt() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("dagsatsFraBg");
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsgiver(null)
            .build(periode);
    }

    @Test
    public void andel_hvor_inntektskategori_ikke_er_satt() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("inntektskategori");
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medDagsatsFraBg(550)
            .medArbeidsgiver(null)
            .build(periode);
    }

    @Test
    public void andel_hvor_utbetalingsgraden_er_minus_1() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Utviklerfeil: Utbetalingsgrad må være mellom 0 og 100");
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(-1))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsatsFraBg(550)
            .medArbeidsgiver(null)
            .build(periode);
    }

    @Test
    public void andel_hvor_utbetalingsgraden_er_101() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Utviklerfeil: Utbetalingsgrad må være mellom 0 og 100");
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(101))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsatsFraBg(550)
            .medArbeidsgiver(null)
            .build(periode);
    }

    @Test
    public void andel_hvor_inntektskategorien_er_udefinert() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(String.format(
            "Utvikler feil: Mangler mapping for inntektskategori %s", Inntektskategori.UDEFINERT));
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(550)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.UDEFINERT)
            .medDagsatsFraBg(550)
            .medArbeidsgiver(null)
            .build(periode);
    }

}
