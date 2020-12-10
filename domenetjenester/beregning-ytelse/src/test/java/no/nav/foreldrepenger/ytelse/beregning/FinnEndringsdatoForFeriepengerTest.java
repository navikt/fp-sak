package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.FinnEndringsdatoForFeriepenger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class FinnEndringsdatoForFeriepengerTest {
    BeregningsresultatEntitet originaltAggregat;
    BeregningsresultatAndel originalAndel;
    BeregningsresultatEntitet revurderingAggregat;
    BeregningsresultatAndel revurderingAndel;
    BeregningsresultatFeriepenger originaleFeriepenger;
    BeregningsresultatFeriepenger revurderingFeriepenger;

    @BeforeEach
    public void setup() {
        // Dummy verdier som kreves for å sette opp feriepenger
        originaltAggregat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        BeregningsresultatPeriode periodeOriginal = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.of(2019, Month.JANUARY, 31), LocalDate.of(2020, Month.JANUARY, 31))
            .build(originaltAggregat);
        originalAndel = lagAndel(periodeOriginal);
        revurderingAggregat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        BeregningsresultatPeriode periodeRevurdering = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.of(2019, Month.JANUARY, 31), LocalDate.of(2020, Month.JANUARY, 31))
            .build(revurderingAggregat);
        revurderingAndel = lagAndel(periodeRevurdering);

        originaleFeriepenger = BeregningsresultatFeriepenger.builder()
            .medFeriepengerRegelInput("")
            .medFeriepengerRegelSporing("")
            .build(originaltAggregat);
        revurderingFeriepenger = BeregningsresultatFeriepenger.builder()
            .medFeriepengerRegelInput("")
            .medFeriepengerRegelSporing("")
            .build(revurderingAggregat);
    }

    private BeregningsresultatAndel lagAndel(BeregningsresultatPeriode forPeriode) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medDagsats(0)
            .medDagsatsFraBg(0)
            .medStillingsprosent(BigDecimal.valueOf(0))
            .medUtbetalingsgrad(BigDecimal.valueOf(0))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsgiver(null)
            .build(forPeriode);
    }

    @Test
    public void ingen_aggregat_gir_ingen_endring() {
        Optional<LocalDate> endringsdato = FinnEndringsdatoForFeriepenger.finnEndringsdato(Optional.empty(), Optional.empty());

        assertThat(endringsdato).isEmpty();
    }

    @Test
    public void kun_originalt_aggregat_gir_endring_fra_første_utbetaling() {
        originalAndel(2020, 50000);
        Optional<LocalDate> endringsdato = FinnEndringsdatoForFeriepenger.finnEndringsdato(Optional.of(originaleFeriepenger), Optional.empty());

        assertThat(endringsdato).isPresent();
        assertThat(endringsdato.get()).isEqualTo(førsteMai(2021));
    }

    @Test
    public void kun_revurdert_aggregat_gir_endring_fra_første_utbetaling() {
        revurderingAndel(2020, 50000);
        Optional<LocalDate> endringsdato = FinnEndringsdatoForFeriepenger.finnEndringsdato(Optional.empty(), Optional.of(revurderingFeriepenger));

        assertThat(endringsdato).isPresent();
        assertThat(endringsdato.get()).isEqualTo(førsteMai(2021));
    }

    @Test
    public void ingen_endring_gir_ingen_dato() {
        originalAndel(2020, 45000);
        originalAndel(2021, 45000);
        revurderingAndel(2020, 45000);
        revurderingAndel(2021, 45000);

        Optional<LocalDate> endringsdato = kjørUtleder();

        assertThat(endringsdato).isEmpty();
    }

    @Test
    public void endring_i_et_år_oppdages() {
        originalAndel(2020, 45000);
        originalAndel(2021, 45000);
        revurderingAndel(2020, 45000);
        revurderingAndel(2021, 45001);

        Optional<LocalDate> endringsdato = kjørUtleder();

        assertThat(endringsdato).isPresent();
        assertThat(endringsdato.get()).isEqualTo(førsteMai(2022));
    }

    @Test
    public void endring_i_flere_perioder_gir_første_kronologiske_dato() {
        originalAndel(2020, 30000);
        originalAndel(2021, 33000);
        originalAndel(2022, 1000);
        revurderingAndel(2020, 12000);
        revurderingAndel(2021, 37000);
        revurderingAndel(2022, 1);

        Optional<LocalDate> endringsdato = kjørUtleder();

        assertThat(endringsdato).isPresent();
        assertThat(endringsdato.get()).isEqualTo(førsteMai(2021));
    }

    private LocalDate førsteMai(int år) {
        return LocalDate.of(år, 5, 1);
    }


    private Optional<LocalDate> kjørUtleder() {
        return FinnEndringsdatoForFeriepenger.finnEndringsdato(Optional.of(originaleFeriepenger), Optional.of(revurderingFeriepenger));
    }

    private void revurderingAndel(int år, int beløp) {
        BeregningsresultatFeriepengerPrÅr.builder()
            .medOpptjeningsår(år)
            .medÅrsbeløp(beløp)
            .build(revurderingFeriepenger, revurderingAndel);
    }

    private void originalAndel(int år, int beløp) {
        BeregningsresultatFeriepengerPrÅr.builder()
            .medOpptjeningsår(år)
            .medÅrsbeløp(beløp)
            .build(originaleFeriepenger, originalAndel);
    }

}
