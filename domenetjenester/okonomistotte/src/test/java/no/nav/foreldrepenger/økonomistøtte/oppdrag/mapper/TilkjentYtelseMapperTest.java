package no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Satsen;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Utbetalingsgrad;


class TilkjentYtelseMapperTest {

    LocalDate jan1 = LocalDate.of(2020, 1, 1);
    LocalDate jan2 = LocalDate.of(2020, 1, 2);
    LocalDate jan3 = LocalDate.of(2020, 1, 3);
    LocalDate jan4 = LocalDate.of(2020, 1, 4);

    LocalDate nesteMai1 = LocalDate.of(2021, 5, 1);
    LocalDate nesteMai31 = LocalDate.of(2021, 5, 31);

    TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
    KodeKlassifik kodeFrilanser = KodeKlassifik.FPF_FRILANSER;

    Arbeidsgiver arbeidsgiver1 = Arbeidsgiver.virksomhet("111111111");

    BeregningsresultatEntitet entitet = new BeregningsresultatEntitet();

    @Test
    void skal_mappe_en_periode_med_en_andel_til_en_kjede_med_en_periode() {
        var periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(jan1, jan2).build(entitet);
        lagAndelTilBruker(Inntektskategori.FRILANSER, 100, periode1);
        var perioder = Arrays.asList(periode1);

        var resultat = mapper.fordelPåNøkler(perioder).getYtelsePrNøkkel();

        Assertions.assertThat(resultat).hasSize(1);

        var forventetNøkkel = KjedeNøkkel.lag(kodeFrilanser, Betalingsmottaker.BRUKER);
        Assertions.assertThat(resultat.keySet()).contains(forventetNøkkel);

        var ytelse = resultat.get(forventetNøkkel);
        Assertions.assertThat(ytelse.getPerioder()).hasSize(1);

        var ytelsePeriode = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode.getSats()).isEqualTo(Satsen.dagsats(100));
        Assertions.assertThat(ytelsePeriode.getPeriode()).isEqualTo(Periode.of(jan1, jan2));
        Assertions.assertThat(ytelsePeriode.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));
    }

    @Test
    void skal_mappe_utbetalingsgrad_riktig_summeres() {
        var periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(jan1, jan2).build(entitet);

        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsats(100)
            .medDagsatsFraBg(100)
            .medUtbetalingsgrad(BigDecimal.valueOf(60))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(periode1);

        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER)
            .medDagsats(100)
            .medDagsatsFraBg(100)
            .medUtbetalingsgrad(BigDecimal.valueOf(50))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(periode1);

        var perioder = Arrays.asList(periode1);

        var resultat = mapper.fordelPåNøkler(perioder).getYtelsePrNøkkel();

        Assertions.assertThat(resultat).hasSize(1);

        var forventetNøkkel = KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        Assertions.assertThat(resultat.keySet()).contains(forventetNøkkel);

        var ytelse = resultat.get(forventetNøkkel);
        Assertions.assertThat(ytelse.getPerioder()).hasSize(1);

        var ytelsePeriode = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode.getSats()).isEqualTo(Satsen.dagsats(200));
        Assertions.assertThat(ytelsePeriode.getPeriode()).isEqualTo(Periode.of(jan1, jan2));
        Assertions.assertThat(ytelsePeriode.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));
    }

    @Test
    void skal_slå_sammen_andeler_som_mapper_til_samme_mottaker_og_klassekode() {
        var periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(jan1, jan2).build(entitet);
        lagAndelTilBruker(Inntektskategori.DAGPENGER, 100, periode1);
        lagAndelTilBruker(Inntektskategori.ARBEIDSAVKLARINGSPENGER, 200, periode1);
        var perioder = Arrays.asList(periode1);

        var resultat = mapper.fordelPåNøkler(perioder).getYtelsePrNøkkel();

        Assertions.assertThat(resultat).hasSize(1);

        var kodeklasse = KodeKlassifik.fraKode("FPATAL");
        var forventetNøkkel = KjedeNøkkel.lag(kodeklasse, Betalingsmottaker.BRUKER);
        Assertions.assertThat(resultat.keySet()).contains(forventetNøkkel);

        var ytelse = resultat.get(forventetNøkkel);
        Assertions.assertThat(ytelse.getPerioder()).hasSize(1);

        var ytelsePeriode = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode.getSats()).isEqualTo(Satsen.dagsats(100 + 200));
        Assertions.assertThat(ytelsePeriode.getPeriode()).isEqualTo(Periode.of(jan1, jan2));
        Assertions.assertThat(ytelsePeriode.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));
    }

    @Test
    void skal_ignore_andel_med_0_dagsats() {
        var periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(jan1, jan1).build(entitet);
        lagAndelTilBruker(Inntektskategori.DAGPENGER, 0, periode1);
        var periode2 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(jan2, jan2).build(entitet);
        lagAndelTilBruker(Inntektskategori.DAGPENGER, 0, periode2);
        lagAndelTilBruker(Inntektskategori.FRILANSER, 1, periode2);
        var perioder = Arrays.asList(periode1, periode2);

        var resultat = mapper.fordelPåNøkler(perioder).getYtelsePrNøkkel();

        Assertions.assertThat(resultat).hasSize(1);

        var kodeklasse = KodeKlassifik.fraKode("FPATFRI");
        var forventetNøkkel = KjedeNøkkel.lag(kodeklasse, Betalingsmottaker.BRUKER);
        Assertions.assertThat(resultat.keySet()).contains(forventetNøkkel);

        var ytelse = resultat.get(forventetNøkkel);
        Assertions.assertThat(ytelse.getPerioder()).hasSize(1);

        var ytelsePeriode = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode.getSats()).isEqualTo(Satsen.dagsats(1));
        Assertions.assertThat(ytelsePeriode.getPeriode()).isEqualTo(Periode.of(jan2, jan2));
    }

    @Test
    void skal_mappe_refusjon_inkl_feriepenger() {
        var brFeriepenger = BeregningsresultatFeriepenger.builder()
            .medFeriepengerPeriodeFom(LocalDate.of(2021, 5, 1))
            .medFeriepengerPeriodeTom(LocalDate.of(2021, 5, 31))
            .medFeriepengerRegelInput("foo")
            .medFeriepengerRegelSporing("bar")
            .build(entitet);

        var periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(jan1, jan2).build(entitet);
        var andel1 = lagAndelTilOrg(arbeidsgiver1, 1000, periode1);
        BeregningsresultatFeriepengerPrÅr.builder().medOpptjeningsår(2020).medÅrsbeløp(204).build(brFeriepenger, andel1);

        var periode2 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(jan3, jan4).build(entitet);
        var andel2 = lagAndelTilOrg(arbeidsgiver1, 1500, periode2);
        BeregningsresultatFeriepengerPrÅr.builder().medOpptjeningsår(2020).medÅrsbeløp(408).build(brFeriepenger, andel2);
        var perioder = Arrays.asList(periode1, periode2);

        //act
        var resultat = mapper.fordelPåNøkler(perioder).getYtelsePrNøkkel();

        //assert
        var forventetNøkkelYtelse = KjedeNøkkel.lag(KodeKlassifik.FPF_REFUSJON_AG, Betalingsmottaker.forArbeidsgiver("111111111"));
        var forventetNøkkelFeriepenger = KjedeNøkkel.lag(KodeKlassifik.FPF_FERIEPENGER_AG, Betalingsmottaker.forArbeidsgiver("111111111"), 2020);
        Assertions.assertThat(resultat.keySet()).containsOnly(forventetNøkkelYtelse, forventetNøkkelFeriepenger);

        var ytelse = resultat.get(forventetNøkkelYtelse);
        Assertions.assertThat(ytelse.getPerioder()).hasSize(2);
        var ytelsePeriode1 = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode1.getSats()).isEqualTo(Satsen.dagsats(1000));
        Assertions.assertThat(ytelsePeriode1.getPeriode()).isEqualTo(Periode.of(jan1, jan2));
        Assertions.assertThat(ytelsePeriode1.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));
        var ytelsePeriode2 = ytelse.getPerioder().get(1);
        Assertions.assertThat(ytelsePeriode2.getSats()).isEqualTo(Satsen.dagsats(1500));
        Assertions.assertThat(ytelsePeriode2.getPeriode()).isEqualTo(Periode.of(jan3, jan4));
        Assertions.assertThat(ytelsePeriode2.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));

        var feriepenger = resultat.get(forventetNøkkelFeriepenger);
        Assertions.assertThat(feriepenger.getPerioder()).hasSize(1);
        var feriepengerPeriode = feriepenger.getPerioder().get(0);
        Assertions.assertThat(feriepengerPeriode.getSats()).isEqualTo(Satsen.engang(204 + 408));
        Assertions.assertThat(feriepengerPeriode.getPeriode()).isEqualTo(Periode.of(nesteMai1, nesteMai31));
        Assertions.assertThat(feriepengerPeriode.getUtbetalingsgrad()).isNull();
    }

    @Test
    void skal_mappe_andeler_fra_private_arbeidsgivere_til_bruker() {
        var periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(jan1, jan2).build(entitet);
        lagAndelTilBruker(Inntektskategori.ARBEIDSTAKER, 1000, periode1);
        lagAndelTilOrg(Arbeidsgiver.person(new AktørId(1234567891234L)), 500, periode1);
        var periode2 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(jan3, jan4).build(entitet);
        lagAndelTilBruker(Inntektskategori.ARBEIDSTAKER, 1000, periode2);
        lagAndelTilOrg(Arbeidsgiver.person(new AktørId(1234567891234L)), 500, periode2);
        var perioder = Arrays.asList(periode1, periode2);

        var resultat = mapper.fordelPåNøkler(perioder).getYtelsePrNøkkel();

        //assert
        var forventetNøkkelYtelse = KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        Assertions.assertThat(resultat.keySet()).containsOnly(forventetNøkkelYtelse);

        var ytelse = resultat.get(forventetNøkkelYtelse);
        Assertions.assertThat(ytelse.getPerioder()).hasSize(2);
        var ytelsePeriode1 = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode1.getSats()).isEqualTo(Satsen.dagsats(1500));
        Assertions.assertThat(ytelsePeriode1.getPeriode()).isEqualTo(Periode.of(jan1, jan2));
        Assertions.assertThat(ytelsePeriode1.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));

        var ytelsePeriode2 = ytelse.getPerioder().get(1);
        Assertions.assertThat(ytelsePeriode2.getSats()).isEqualTo(Satsen.dagsats(1500));
        Assertions.assertThat(ytelsePeriode2.getPeriode()).isEqualTo(Periode.of(jan3, jan4));
        Assertions.assertThat(ytelsePeriode2.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));
    }

    private static BeregningsresultatAndel lagAndelTilBruker(Inntektskategori inntektskategori, int dagsats, BeregningsresultatPeriode periode) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medInntektskategori(inntektskategori)
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(periode);
    }

    private static BeregningsresultatAndel lagAndelTilOrg(Arbeidsgiver virksomhet, int dagsats, BeregningsresultatPeriode periode) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(false)
            .medArbeidsgiver(virksomhet)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(periode);
    }
}
