package no.nav.foreldrepenger.økonomi.ny.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.økonomi.ny.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomi.ny.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomi.ny.domene.Periode;
import no.nav.foreldrepenger.økonomi.ny.domene.Sats;
import no.nav.foreldrepenger.økonomi.ny.domene.Utbetalingsgrad;
import no.nav.foreldrepenger.økonomi.ny.domene.Ytelse;
import no.nav.foreldrepenger.økonomi.ny.domene.YtelsePeriode;


public class TilkjentYtelseMapperTest {

    LocalDate jan1 = LocalDate.of(2020, 1, 1);
    LocalDate jan2 = LocalDate.of(2020, 1, 2);
    LocalDate jan3 = LocalDate.of(2020, 1, 3);
    LocalDate jan4 = LocalDate.of(2020, 1, 4);

    LocalDate nesteMai1 = LocalDate.of(2021, 5, 1);
    LocalDate nesteMai31 = LocalDate.of(2021, 5, 31);

    TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FagsakYtelseType.FORELDREPENGER, FamilieYtelseType.FØDSEL);
    ØkonomiKodeKlassifik kodeFrilanser = ØkonomiKodeKlassifik.FPATFRI;

    Arbeidsgiver arbeidsgiver1 = Arbeidsgiver.virksomhet("111111111");

    BeregningsresultatEntitet entitet = new BeregningsresultatEntitet();

    @Test
    public void skal_mappe_en_periode_med_en_andel_til_en_kjede_med_en_periode() {
        BeregningsresultatPeriode periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(jan1, jan2).build(entitet);
        lagAndelTilBruker(Inntektskategori.FRILANSER, 100, periode1);
        List<BeregningsresultatPeriode> perioder = Arrays.asList(periode1);

        Map<KjedeNøkkel, Ytelse> resultat = mapper.fordelPåNøkler(perioder).getYtelsePrNøkkel();

        Assertions.assertThat(resultat).hasSize(1);

        KjedeNøkkel forventetNøkkel = KjedeNøkkel.lag(kodeFrilanser, Betalingsmottaker.BRUKER);
        Assertions.assertThat(resultat.keySet()).contains(forventetNøkkel);

        Ytelse ytelse = resultat.get(forventetNøkkel);
        Assertions.assertThat(ytelse.getPerioder()).hasSize(1);

        YtelsePeriode ytelsePeriode = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode.getSats()).isEqualTo(Sats.dagsats(100));
        Assertions.assertThat(ytelsePeriode.getPeriode()).isEqualTo(Periode.of(jan1, jan2));
        Assertions.assertThat(ytelsePeriode.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));
    }

    @Test
    public void skal_slå_sammen_andeler_som_mapper_til_samme_mottaker_og_klassekode() {
        BeregningsresultatPeriode periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(jan1, jan2).build(entitet);
        lagAndelTilBruker(Inntektskategori.DAGPENGER, 100, periode1);
        lagAndelTilBruker(Inntektskategori.ARBEIDSAVKLARINGSPENGER, 200, periode1);
        List<BeregningsresultatPeriode> perioder = Arrays.asList(periode1);

        Map<KjedeNøkkel, Ytelse> resultat = mapper.fordelPåNøkler(perioder).getYtelsePrNøkkel();

        Assertions.assertThat(resultat).hasSize(1);

        ØkonomiKodeKlassifik kodeklasse = ØkonomiKodeKlassifik.fraKode("FPATAL");
        KjedeNøkkel forventetNøkkel = KjedeNøkkel.lag(kodeklasse, Betalingsmottaker.BRUKER);
        Assertions.assertThat(resultat.keySet()).contains(forventetNøkkel);

        Ytelse ytelse = resultat.get(forventetNøkkel);
        Assertions.assertThat(ytelse.getPerioder()).hasSize(1);

        YtelsePeriode ytelsePeriode = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode.getSats()).isEqualTo(Sats.dagsats(100 + 200));
        Assertions.assertThat(ytelsePeriode.getPeriode()).isEqualTo(Periode.of(jan1, jan2));
        Assertions.assertThat(ytelsePeriode.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));
    }

    @Test
    public void skal_ignore_andel_med_0_dagsats() {
        BeregningsresultatPeriode periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(jan1, jan1).build(entitet);
        lagAndelTilBruker(Inntektskategori.DAGPENGER, 0, periode1);
        BeregningsresultatPeriode periode2 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(jan2, jan2).build(entitet);
        lagAndelTilBruker(Inntektskategori.DAGPENGER, 0, periode2);
        lagAndelTilBruker(Inntektskategori.FRILANSER, 1, periode2);
        List<BeregningsresultatPeriode> perioder = Arrays.asList(periode1, periode2);

        Map<KjedeNøkkel, Ytelse> resultat = mapper.fordelPåNøkler(perioder).getYtelsePrNøkkel();

        Assertions.assertThat(resultat).hasSize(1);

        ØkonomiKodeKlassifik kodeklasse = ØkonomiKodeKlassifik.fraKode("FPATFRI");
        KjedeNøkkel forventetNøkkel = KjedeNøkkel.lag(kodeklasse, Betalingsmottaker.BRUKER);
        Assertions.assertThat(resultat.keySet()).contains(forventetNøkkel);

        Ytelse ytelse = resultat.get(forventetNøkkel);
        Assertions.assertThat(ytelse.getPerioder()).hasSize(1);

        YtelsePeriode ytelsePeriode = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode.getSats()).isEqualTo(Sats.dagsats(1));
        Assertions.assertThat(ytelsePeriode.getPeriode()).isEqualTo(Periode.of(jan2, jan2));
    }

    @Test
    public void skal_mappe_refusjon_inkl_feriepenger() {
        BeregningsresultatFeriepenger brFeriepenger = BeregningsresultatFeriepenger.builder()
            .medFeriepengerPeriodeFom(LocalDate.of(2021, 5, 1))
            .medFeriepengerPeriodeTom(LocalDate.of(2021, 5, 31))
            .medFeriepengerRegelInput("foo")
            .medFeriepengerRegelSporing("bar")
            .build(entitet);

        BeregningsresultatPeriode periode1 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(jan1, jan2).build(entitet);
        BeregningsresultatAndel andel1 = lagAndelTilOrg(arbeidsgiver1, 1000, periode1);
        BeregningsresultatFeriepengerPrÅr.builder().medOpptjeningsår(2020).medÅrsbeløp(204).build(brFeriepenger, andel1);

        BeregningsresultatPeriode periode2 = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(jan3, jan4).build(entitet);
        BeregningsresultatAndel andel2 = lagAndelTilOrg(arbeidsgiver1, 1500, periode2);
        BeregningsresultatFeriepengerPrÅr.builder().medOpptjeningsår(2020).medÅrsbeløp(408).build(brFeriepenger, andel2);
        List<BeregningsresultatPeriode> perioder = Arrays.asList(periode1, periode2);

        //act
        Map<KjedeNøkkel, Ytelse> resultat = mapper.fordelPåNøkler(perioder).getYtelsePrNøkkel();

        //assert
        KjedeNøkkel forventetNøkkelYtelse = KjedeNøkkel.lag(ØkonomiKodeKlassifik.FPREFAG_IOP, Betalingsmottaker.forArbeidsgiver("111111111"));
        KjedeNøkkel forventetNøkkelFeriepenger = KjedeNøkkel.lag(ØkonomiKodeKlassifik.FPREFAGFER_IOP, Betalingsmottaker.forArbeidsgiver("111111111"), 2020);
        Assertions.assertThat(resultat.keySet()).containsOnly(forventetNøkkelYtelse, forventetNøkkelFeriepenger);

        Ytelse ytelse = resultat.get(forventetNøkkelYtelse);
        Assertions.assertThat(ytelse.getPerioder()).hasSize(2);
        YtelsePeriode ytelsePeriode1 = ytelse.getPerioder().get(0);
        Assertions.assertThat(ytelsePeriode1.getSats()).isEqualTo(Sats.dagsats(1000));
        Assertions.assertThat(ytelsePeriode1.getPeriode()).isEqualTo(Periode.of(jan1, jan2));
        Assertions.assertThat(ytelsePeriode1.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));
        YtelsePeriode ytelsePeriode2 = ytelse.getPerioder().get(1);
        Assertions.assertThat(ytelsePeriode2.getSats()).isEqualTo(Sats.dagsats(1500));
        Assertions.assertThat(ytelsePeriode2.getPeriode()).isEqualTo(Periode.of(jan3, jan4));
        Assertions.assertThat(ytelsePeriode2.getUtbetalingsgrad()).isEqualTo(new Utbetalingsgrad(100));

        Ytelse feriepenger = resultat.get(forventetNøkkelFeriepenger);
        Assertions.assertThat(feriepenger.getPerioder()).hasSize(1);
        YtelsePeriode feriepengerPeriode = feriepenger.getPerioder().get(0);
        Assertions.assertThat(feriepengerPeriode.getSats()).isEqualTo(Sats.engang(204 + 408));
        Assertions.assertThat(feriepengerPeriode.getPeriode()).isEqualTo(Periode.of(nesteMai1, nesteMai31));
        Assertions.assertThat(feriepengerPeriode.getUtbetalingsgrad()).isNull();
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
