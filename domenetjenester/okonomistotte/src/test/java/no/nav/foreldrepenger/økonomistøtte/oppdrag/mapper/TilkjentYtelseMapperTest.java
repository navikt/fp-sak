package no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
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

    private static final LocalDate JAN_1 = LocalDate.of(2020, 1, 1);
    private static final LocalDate JAN_2 = LocalDate.of(2020, 1, 2);
    private static final LocalDate JAN_3 = LocalDate.of(2020, 1, 3); // Fredag
    private static final LocalDate JAN_4 = LocalDate.of(2020, 1, 4);
    private static final LocalDate JAN_6 = LocalDate.of(2020, 1, 6); // Mandag
    private static final LocalDate JAN_7 = LocalDate.of(2020, 1, 7);

    private static final LocalDate NESTE_MAI_1 = LocalDate.of(2021, 5, 1);
    private static final LocalDate NESTE_MAI_31 = LocalDate.of(2021, 5, 31);

    private static final TilkjentYtelseMapper MAPPER = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
    private static final KodeKlassifik KODE_FRILANSER = KodeKlassifik.FPF_FRILANSER;

    private static final Arbeidsgiver ARBEIDSGIVER_1 = Arbeidsgiver.virksomhet("111111111");

    private static final BeregningsresultatEntitet ENTITET = new BeregningsresultatEntitet();

    @Test
    void skal_mappe_en_periode_med_en_andel_til_en_kjede_med_en_periode() {
        var periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(JAN_1, JAN_2).build(ENTITET);
        lagAndelTilBruker(Inntektskategori.FRILANSER, 100, periode1);
        var perioder = List.of(periode1);

        var resultat = MAPPER.fordelPåNøkler(perioder, List.of()).getYtelsePrNøkkel();

        Assertions.assertThat(resultat).hasSize(1);

        var forventetNøkkel = KjedeNøkkel.lag(KODE_FRILANSER, Betalingsmottaker.BRUKER);
        Assertions.assertThat(resultat).containsKey(forventetNøkkel);

        var ytelse = resultat.get(forventetNøkkel);
        Assertions.assertThat(ytelse.getPerioder()).hasSize(1);

        var ytelsePeriode = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode.getSats()).isEqualTo(Satsen.dagsats(100));
        Assertions.assertThat(ytelsePeriode.getPeriode()).isEqualTo(Periode.of(JAN_1, JAN_2));
        Assertions.assertThat(ytelsePeriode.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));
    }

    @Test
    void skal_mappe_utbetalingsgrad_riktig_summeres() {
        var periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(JAN_1, JAN_2).build(ENTITET);

        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(100)
            .medDagsatsFraBg(100)
            .medUtbetalingsgrad(BigDecimal.valueOf(60))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(periode1);

        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(100)
            .medDagsatsFraBg(100)
            .medUtbetalingsgrad(BigDecimal.valueOf(50))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(periode1);

        var perioder = List.of(periode1);

        var resultat = MAPPER.fordelPåNøkler(perioder, List.of()).getYtelsePrNøkkel();

        Assertions.assertThat(resultat).hasSize(1);

        var forventetNøkkel = KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        Assertions.assertThat(resultat).containsKey(forventetNøkkel);

        var ytelse = resultat.get(forventetNøkkel);
        Assertions.assertThat(ytelse.getPerioder()).hasSize(1);

        var ytelsePeriode = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode.getSats()).isEqualTo(Satsen.dagsats(200));
        Assertions.assertThat(ytelsePeriode.getPeriode()).isEqualTo(Periode.of(JAN_1, JAN_2));
        Assertions.assertThat(ytelsePeriode.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));
    }

    @Test
    void skal_slå_sammen_andeler_som_mapper_til_samme_mottaker_og_klassekode() {
        var periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(JAN_1, JAN_2).build(ENTITET);
        lagAndelTilBruker(Inntektskategori.DAGPENGER, 100, periode1);
        lagAndelTilBruker(Inntektskategori.ARBEIDSAVKLARINGSPENGER, 200, periode1);
        var perioder = List.of(periode1);

        var resultat = MAPPER.fordelPåNøkler(perioder, List.of()).getYtelsePrNøkkel();

        Assertions.assertThat(resultat).hasSize(1);

        var kodeklasse = KodeKlassifik.FPF_DAGPENGER;
        var forventetNøkkel = KjedeNøkkel.lag(kodeklasse, Betalingsmottaker.BRUKER);
        Assertions.assertThat(resultat).containsKey(forventetNøkkel);

        var ytelse = resultat.get(forventetNøkkel);
        Assertions.assertThat(ytelse.getPerioder()).hasSize(1);

        var ytelsePeriode = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode.getSats()).isEqualTo(Satsen.dagsats(100 + 200));
        Assertions.assertThat(ytelsePeriode.getPeriode()).isEqualTo(Periode.of(JAN_1, JAN_2));
        Assertions.assertThat(ytelsePeriode.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));
    }

    @Test
    void skal_ignore_andel_med_0_dagsats() {
        var periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(JAN_1, JAN_1).build(ENTITET);
        lagAndelTilBruker(Inntektskategori.DAGPENGER, 0, periode1);
        var periode2 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(JAN_2, JAN_2).build(ENTITET);
        lagAndelTilBruker(Inntektskategori.DAGPENGER, 0, periode2);
        lagAndelTilBruker(Inntektskategori.FRILANSER, 1, periode2);
        var perioder = List.of(periode1, periode2);

        var resultat = MAPPER.fordelPåNøkler(perioder, List.of()).getYtelsePrNøkkel();

        Assertions.assertThat(resultat).hasSize(1);

        var kodeklasse = KodeKlassifik.FPF_FRILANSER;
        var forventetNøkkel = KjedeNøkkel.lag(kodeklasse, Betalingsmottaker.BRUKER);
        Assertions.assertThat(resultat).containsKey(forventetNøkkel);

        var ytelse = resultat.get(forventetNøkkel);
        Assertions.assertThat(ytelse.getPerioder()).hasSize(1);

        var ytelsePeriode = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode.getSats()).isEqualTo(Satsen.dagsats(1));
        Assertions.assertThat(ytelsePeriode.getPeriode()).isEqualTo(Periode.of(JAN_2, JAN_2));
    }

    @Test
    void skal_mappe_refusjon_inkl_feriepenger() {
        var brFeriepenger = BeregningsresultatFeriepenger.builder()
            .medFeriepengerPeriodeFom(LocalDate.of(2021, 5, 1))
            .medFeriepengerPeriodeTom(LocalDate.of(2021, 5, 31))
            .medFeriepengerRegelInput("foo")
            .medFeriepengerRegelSporing("bar")
            .build();

        var periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(JAN_1, JAN_2).build(ENTITET);
        var andel1 = lagAndelTilOrg(ARBEIDSGIVER_1, 1000, periode1);
        var feriepenger1 = BeregningsresultatFeriepengerPrÅr.builder()
            .medAktivitetStatus(andel1.getAktivitetStatus()).medBrukerErMottaker(andel1.erBrukerMottaker())
            .medArbeidsgiver(andel1.getArbeidsgiver().orElse(null)).medArbeidsforholdRef(andel1.getArbeidsforholdRef())
            .medOpptjeningsår(2020).medÅrsbeløp(204).build(brFeriepenger);

        var periode2 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(JAN_3, JAN_4).build(ENTITET);
        var andel2 = lagAndelTilOrg(ARBEIDSGIVER_1, 1500, periode2);
        var feriepenger2 = BeregningsresultatFeriepengerPrÅr.builder()
            .medAktivitetStatus(andel2.getAktivitetStatus()).medBrukerErMottaker(andel2.erBrukerMottaker())
            .medArbeidsgiver(andel2.getArbeidsgiver().orElse(null)).medArbeidsforholdRef(andel2.getArbeidsforholdRef())
            .medOpptjeningsår(2020).medÅrsbeløp(408).build(brFeriepenger);
        var perioder = List.of(periode1, periode2);

        //act
        var resultat = MAPPER.fordelPåNøkler(perioder, List.of(feriepenger1, feriepenger2)).getYtelsePrNøkkel();

        //assert
        var forventetNøkkelYtelse = KjedeNøkkel.lag(KodeKlassifik.FPF_REFUSJON_AG, Betalingsmottaker.forArbeidsgiver("111111111"));
        var forventetNøkkelFeriepenger = KjedeNøkkel.lag(KodeKlassifik.FPF_FERIEPENGER_AG, Betalingsmottaker.forArbeidsgiver("111111111"), 2020);
        Assertions.assertThat(resultat).containsOnlyKeys(forventetNøkkelYtelse, forventetNøkkelFeriepenger);

        var ytelse = resultat.get(forventetNøkkelYtelse);
        Assertions.assertThat(ytelse.getPerioder()).hasSize(2);
        var ytelsePeriode1 = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode1.getSats()).isEqualTo(Satsen.dagsats(1000));
        Assertions.assertThat(ytelsePeriode1.getPeriode()).isEqualTo(Periode.of(JAN_1, JAN_2));
        Assertions.assertThat(ytelsePeriode1.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));
        var ytelsePeriode2 = ytelse.getPerioder().get(1);
        Assertions.assertThat(ytelsePeriode2.getSats()).isEqualTo(Satsen.dagsats(1500));
        Assertions.assertThat(ytelsePeriode2.getPeriode()).isEqualTo(Periode.of(JAN_3, JAN_4));
        Assertions.assertThat(ytelsePeriode2.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));

        var feriepenger = resultat.get(forventetNøkkelFeriepenger);
        Assertions.assertThat(feriepenger.getPerioder()).hasSize(1);
        var feriepengerPeriode = feriepenger.getPerioder().get(0);
        Assertions.assertThat(feriepengerPeriode.getSats()).isEqualTo(Satsen.engang(204 + 408));
        Assertions.assertThat(feriepengerPeriode.getPeriode()).isEqualTo(Periode.of(NESTE_MAI_1, NESTE_MAI_31));
        Assertions.assertThat(feriepengerPeriode.getUtbetalingsgrad()).isNull();
    }

    @Test
    void skal_slå_sammen_perioder_som_er_tidsnaboer_med_like_verdier() {
        var periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(JAN_1, JAN_2).build(ENTITET);
        lagAndelTilBruker(Inntektskategori.ARBEIDSTAKER, 1000, periode1);
        var periode2 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(JAN_3, JAN_4).build(ENTITET);
        lagAndelTilBruker(Inntektskategori.ARBEIDSTAKER, 1000, periode2);
        var perioder = List.of(periode1, periode2);

        var resultat = MAPPER.fordelPåNøkler(perioder, List.of()).getYtelsePrNøkkel();

        //assert
        var forventetNøkkelYtelse = KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        Assertions.assertThat(resultat).containsOnlyKeys(forventetNøkkelYtelse);

        var ytelse = resultat.get(forventetNøkkelYtelse);
        // Skal være komprimert til 1 sammenhengende periode
        Assertions.assertThat(ytelse.getPerioder()).hasSize(1);
        var ytelsePeriode1 = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode1.getSats()).isEqualTo(Satsen.dagsats(1000));
        Assertions.assertThat(ytelsePeriode1.getPeriode()).isEqualTo(Periode.of(JAN_1, JAN_4));
        Assertions.assertThat(ytelsePeriode1.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));
    }

    @Test
    void skal_slå_sammen_perioder_som_er_tidsnaboer__fredag_mandag_med_like_verdier() {
        var periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(JAN_1, JAN_3).build(ENTITET);
        lagAndelTilBruker(Inntektskategori.ARBEIDSTAKER, 1000, periode1);
        var periode2 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(JAN_6, JAN_7).build(ENTITET);
        lagAndelTilBruker(Inntektskategori.ARBEIDSTAKER, 1000, periode2);
        var perioder = List.of(periode1, periode2);

        var resultat = MAPPER.fordelPåNøkler(perioder, List.of()).getYtelsePrNøkkel();

        //assert
        var forventetNøkkelYtelse = KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        Assertions.assertThat(resultat).containsOnlyKeys(forventetNøkkelYtelse);

        var ytelse = resultat.get(forventetNøkkelYtelse);
        // Skal være komprimert til 1 sammenhengende periode
        Assertions.assertThat(ytelse.getPerioder()).hasSize(1);
        var ytelsePeriode1 = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode1.getSats()).isEqualTo(Satsen.dagsats(1000));
        Assertions.assertThat(ytelsePeriode1.getPeriode()).isEqualTo(Periode.of(JAN_1, JAN_7));
        Assertions.assertThat(ytelsePeriode1.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));
    }

    @Test
    void skal_mappe_andeler_fra_private_arbeidsgivere_til_bruker() {
        var periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(JAN_1, JAN_2).build(ENTITET);
        lagAndelTilBruker(Inntektskategori.ARBEIDSTAKER, 1000, periode1);
        lagAndelTilOrg(Arbeidsgiver.person(new AktørId(1234567891234L)), 500, periode1);
        var periode2 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(JAN_6, JAN_7).build(ENTITET);
        lagAndelTilBruker(Inntektskategori.ARBEIDSTAKER, 1000, periode2);
        lagAndelTilOrg(Arbeidsgiver.person(new AktørId(1234567891234L)), 500, periode2);
        var perioder = List.of(periode1, periode2);

        var resultat = MAPPER.fordelPåNøkler(perioder, List.of()).getYtelsePrNøkkel();

        //assert
        var forventetNøkkelYtelse = KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER);
        Assertions.assertThat(resultat).containsOnlyKeys(forventetNøkkelYtelse);

        var ytelse = resultat.get(forventetNøkkelYtelse);
        // Skal være komprimert til 1 sammenhengende periode
        Assertions.assertThat(ytelse.getPerioder()).hasSize(2);
        var ytelsePeriode1 = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode1.getSats()).isEqualTo(Satsen.dagsats(1500));
        Assertions.assertThat(ytelsePeriode1.getPeriode()).isEqualTo(Periode.of(JAN_1, JAN_2));
        Assertions.assertThat(ytelsePeriode1.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));

        var ytelsePeriode2 = ytelse.getPerioder().get(1);
        Assertions.assertThat(ytelsePeriode2.getSats()).isEqualTo(Satsen.dagsats(1500));
        Assertions.assertThat(ytelsePeriode2.getPeriode()).isEqualTo(Periode.of(JAN_6, JAN_7));
        Assertions.assertThat(ytelsePeriode2.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));
    }

    private static BeregningsresultatAndel lagAndelTilBruker(Inntektskategori inntektskategori, int dagsats, BeregningsresultatPeriode periode) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medInntektskategori(inntektskategori)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
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
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(periode);
    }
}
