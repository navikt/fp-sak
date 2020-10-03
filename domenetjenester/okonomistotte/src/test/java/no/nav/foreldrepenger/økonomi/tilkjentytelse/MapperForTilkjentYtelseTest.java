package no.nav.foreldrepenger.økonomi.tilkjentytelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseAndelV1;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseFeriepengerV1;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelsePeriodeV1;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseV1;

public class MapperForTilkjentYtelseTest {

    @Test
    public void skal_mappe_beregningsresultat_fp() {
        BeregningsresultatEntitet beregningsresultat = lagTilkjentYtelseTilBruker();
        List<TilkjentYtelsePeriodeV1> ty = MapperForTilkjentYtelse.mapTilkjentYtelse(beregningsresultat);
        assertThat(ty).hasSize(1);
        TilkjentYtelsePeriodeV1 periode = ty.get(0);
        assertThat(periode.getFom()).isEqualTo(LocalDate.of(2018, 3, 1));
        assertThat(periode.getTom()).isEqualTo(LocalDate.of(2018, 3, 31));
        assertThat(periode.getAndeler()).hasSize(1);
        TilkjentYtelseAndelV1 andel = periode.getAndeler().iterator().next();
        assertThat(andel.getUtbetalesTilBruker()).isTrue();
        assertThat(andel.getSatsBeløp()).isEqualTo(1000);
        assertThat(andel.getSatsType()).isEqualTo(TilkjentYtelseV1.SatsType.DAGSATS);
        assertThat(andel.getUtbetalingsgrad()).isEqualTo(BigDecimal.valueOf(100));
        List<TilkjentYtelseFeriepengerV1> feriepengerV1List = andel.getFeriepenger();
        assertThat(feriepengerV1List).hasSize(1);
        TilkjentYtelseFeriepengerV1 feriepengerV1 = feriepengerV1List.get(0);
        assertThat(feriepengerV1.getBeløp()).isEqualTo(15000L);
        assertThat(feriepengerV1.getOpptjeningsår()).isEqualTo(2018);
    }

    static BeregningsresultatEntitet lagTilkjentYtelseTilBruker() {
        return lagTilkjentYtelse(andel -> andel.medBrukerErMottaker(true));
    }

    static BeregningsresultatEntitet lagTilkjentYtelse(UnaryOperator<BeregningsresultatAndel.Builder> andelModifier) {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder()
                .medRegelInput("foo")
                .medRegelSporing("bar")
                .build();

        BeregningsresultatPeriode periode = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(LocalDate.of(2018, 3, 1), LocalDate.of(2018, 3, 31))
                .build(beregningsresultat);
        BeregningsresultatAndel.Builder andelBuilder = BeregningsresultatAndel.builder()
                .medDagsats(1000)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medUtbetalingsgrad(BigDecimal.valueOf(100))
                .medStillingsprosent(BigDecimal.valueOf(100))
                .medDagsatsFraBg(1001);

        andelBuilder = andelModifier.apply(andelBuilder);
        BeregningsresultatAndel andel = andelBuilder.build(periode);
        BeregningsresultatFeriepenger ferienger = BeregningsresultatFeriepenger.builder()
                .medFeriepengerRegelInput("foo")
                .medFeriepengerRegelSporing("bar")
                .medFeriepengerPeriodeFom(LocalDate.of(2018, 3, 1))
                .medFeriepengerPeriodeTom(LocalDate.of(2018, 3, 31))
                .build(beregningsresultat);
        BeregningsresultatFeriepengerPrÅr.builder()
                .medOpptjeningsår(LocalDate.of(2018, 12, 31))
                .medÅrsbeløp(15000L)
                .build(ferienger, andel);
        return beregningsresultat;
    }
}
