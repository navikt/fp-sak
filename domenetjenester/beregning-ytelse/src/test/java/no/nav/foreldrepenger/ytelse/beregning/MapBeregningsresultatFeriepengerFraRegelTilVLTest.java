package no.nav.foreldrepenger.ytelse.beregning;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.time.Period;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraRegelTilVL;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.FastsattFeriepengeresultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.fpsak.tidsserie.LocalDateInterval;

class MapBeregningsresultatFeriepengerFraRegelTilVLTest {

    private static final LocalDate STP = LocalDate.now();
    private static final LocalDateInterval PERIODE = LocalDateInterval.withPeriodAfterDate(STP, Period.ofMonths(10));
    public static final String ORGNR = KUNSTIG_ORG;
    private static final Arbeidsforhold ARBEIDSFORHOLD = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(ORGNR, null);
    private static final long DAGSATS = 500L;
    private static final long DAGSATS_FRA_BG = 500L;
    private static final BigDecimal UTBETALINGSGRAD = BigDecimal.valueOf(100);

    @Test
    void skal_ikkje_lage_feriepengeresultat_om_årsbeløp_avrundes_til_0() {
        // Arrange
        var periode = lagPeriodeMedAndel(BigDecimal.valueOf(0.1));
        var beregningsresultat = lagVlBeregningsresultat();
        var regelresultat = new BeregningsresultatFeriepengerResultat(List.of(periode), new LocalDateInterval(STP, STP.plusMonths(10)));
        var resultat = new FastsattFeriepengeresultat(regelresultat, null, "input", "sporing", null);

        // Act
        var feriepenger = MapBeregningsresultatFeriepengerFraRegelTilVL.mapFra(beregningsresultat, resultat);

        // Assert
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe()).isEmpty();
    }

    @Test
    void skal_lage_feriepengeresultat_om_årsbeløp_ikkje_avrundes_til_0() {
        // Arrange
        var periode = lagPeriodeMedAndel(BigDecimal.valueOf(1.5));
        var beregningsresultat = lagVlBeregningsresultat();
        var regelresultat = new BeregningsresultatFeriepengerResultat(List.of(periode), new LocalDateInterval(STP, STP.plusMonths(10)));
        var resultat = new FastsattFeriepengeresultat(regelresultat, null, "input", "sporing", null);

        // Act
        var feriepenger = MapBeregningsresultatFeriepengerFraRegelTilVL.mapFra(beregningsresultat, resultat);


        // Assert
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1);
    }

    private BeregningsresultatEntitet lagVlBeregningsresultat() {
        return BeregningsresultatEntitet
                .builder()
                .medRegelInput("Regelinput")
                .medRegelSporing("Regelsporing")
                .build();
    }

    private BeregningsresultatPeriode lagPeriodeMedAndel(BigDecimal årsbeløp) {
        var andel = BeregningsresultatAndel.builder().medAktivitetStatus(AktivitetStatus.ATFL)
                .medBrukerErMottaker(true)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medArbeidsforhold(ARBEIDSFORHOLD)
                .medDagsats(DAGSATS)
                .medDagsatsFraBg(DAGSATS_FRA_BG)
                .medUtbetalingssgrad(UTBETALINGSGRAD)
                .build();
        andel.addBeregningsresultatFeriepengerPrÅr(fraAndel(andel, årsbeløp, STP));
        var periode = new BeregningsresultatPeriode(PERIODE);
        periode.addBeregningsresultatAndel(andel);
        return periode;
    }

    @Test
    void skal_lage_feriepengeresultat_med_flere_andeler() {
        // Arrange
        var periode = lagPeriodeMedMangeAndeler(PERIODE, STP);
        var beregningsresultat = lagVlBeregningsresultat();
        var regelresultat = new BeregningsresultatFeriepengerResultat(List.of(periode), new LocalDateInterval(STP, STP.plusMonths(10)));
        var resultat = new FastsattFeriepengeresultat(regelresultat, null, "input", "sporing", null);

        // Act
        var feriepenger = MapBeregningsresultatFeriepengerFraRegelTilVL.mapFra(beregningsresultat, resultat);


        // Assert
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(4);
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe().stream().filter(f -> f.erBrukerMottaker()).toList()).hasSize(3);
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe().stream()
            .filter(f -> f.erBrukerMottaker() && f.getArbeidsgiver().isEmpty()).map(f -> f.getÅrsbeløp())
            .reduce(Beløp.ZERO, Beløp::adder).getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(25 + 2 + 3)); // SN + 2 stk FL
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe().stream()
            .filter(f -> f.erBrukerMottaker() && f.getArbeidsgiver().isPresent()).map(f -> f.getÅrsbeløp())
            .reduce(Beløp.ZERO, Beløp::adder).getVerdi()).isEqualByComparingTo(BigDecimal.ONE); // Arbeidsgiver direkte
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe().stream()
            .filter(f -> !f.erBrukerMottaker()).map(f -> f.getÅrsbeløp())
            .reduce(Beløp.ZERO, Beløp::adder).getVerdi()).isEqualByComparingTo(BigDecimal.TEN); // Arbeidsgiver refusjon
    }

    @Test
    void skal_lage_feriepengeresultat_med_flere_perioder() {
        // Arrange
        var baselinedato = LocalDate.of(2023, Month.DECEMBER, 1);
        var baselinedatoP2W = baselinedato.plusWeeks(2);
        var baselinedatoP6W = baselinedato.plusWeeks(6);

        var periode1 = lagPeriodeMedMangeAndeler(new LocalDateInterval(baselinedato, baselinedatoP2W.minusDays(1)), baselinedato);
        var periode2 = lagPeriodeMedMangeAndeler(new LocalDateInterval(baselinedatoP2W, baselinedatoP6W.minusDays(1)), baselinedatoP2W);
        var periode3 = lagPeriodeMedMangeAndeler(new LocalDateInterval(baselinedatoP6W, baselinedato.plusWeeks(8).minusDays(1)), baselinedatoP6W);

        var beregningsresultat = lagVlBeregningsresultat();
        var regelresultat = new BeregningsresultatFeriepengerResultat(List.of(periode1, periode2, periode3),
            new LocalDateInterval(baselinedato, baselinedato.plusWeeks(8).minusDays(1)));
        var resultat = new FastsattFeriepengeresultat(regelresultat, null, "input", "sporing", null);

        // Act
        var feriepenger = MapBeregningsresultatFeriepengerFraRegelTilVL.mapFra(beregningsresultat, resultat);


        // Assert
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(8);
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe().stream().filter(f -> f.erBrukerMottaker()).toList()).hasSize(6);
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe().stream()
            .filter(f -> f.erBrukerMottaker() && f.getOpptjeningsår().getYear() == baselinedato.getYear()).toList()).hasSize(3);
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe().stream()
            .filter(f -> f.erBrukerMottaker() && f.getArbeidsgiver().isEmpty() && f.getOpptjeningsår().getYear() == baselinedato.getYear())
            .map(f -> f.getÅrsbeløp()).reduce(Beløp.ZERO, Beløp::adder).getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(2 * (25 + 2 + 3))); // SN + 2 stk FL
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe().stream()
            .filter(f -> f.erBrukerMottaker() && f.getArbeidsgiver().isPresent() && f.getOpptjeningsår().getYear() == baselinedato.getYear())
            .map(f -> f.getÅrsbeløp()).reduce(Beløp.ZERO, Beløp::adder).getVerdi()).isEqualByComparingTo(BigDecimal.ONE.add(BigDecimal.ONE)); // 2 perioder Arbeidsgiver direkte
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe().stream()
            .filter(f -> !f.erBrukerMottaker() && f.getArbeidsgiver().isPresent() && f.getOpptjeningsår().getYear() == baselinedato.getYear())
            .map(f -> f.getÅrsbeløp()).reduce(Beløp.ZERO, Beløp::adder).getVerdi()).isEqualByComparingTo(BigDecimal.TEN.add(BigDecimal.TEN)); // 2 perioder Arbeidsgiver direkte
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe().stream()
            .filter(f -> f.erBrukerMottaker() && f.getArbeidsgiver().isEmpty() && f.getOpptjeningsår().getYear() == baselinedatoP6W.getYear())
            .map(f -> f.getÅrsbeløp()).reduce(Beløp.ZERO, Beløp::adder).getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(25 + 2 + 3)); // SN + 2 stk FL
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe().stream()
            .filter(f -> f.erBrukerMottaker() && f.getArbeidsgiver().isPresent() && f.getOpptjeningsår().getYear() == baselinedatoP6W.getYear())
            .map(f -> f.getÅrsbeløp()).reduce(Beløp.ZERO, Beløp::adder).getVerdi()).isEqualByComparingTo(BigDecimal.ONE); // Arbeidsgiver direkte
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe().stream()
            .filter(f -> !f.erBrukerMottaker() && f.getArbeidsgiver().isPresent() && f.getOpptjeningsår().getYear() == baselinedatoP6W.getYear())
            .map(f -> f.getÅrsbeløp()).reduce(Beløp.ZERO, Beløp::adder).getVerdi()).isEqualByComparingTo(BigDecimal.TEN); // Arbeidsgiver direkte
    }



    private BeregningsresultatPeriode lagPeriodeMedMangeAndeler(LocalDateInterval tidsperiode, LocalDate opptjeningsår) {
        var andelATD = BeregningsresultatAndel.builder().medAktivitetStatus(AktivitetStatus.ATFL)
            .medBrukerErMottaker(true)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsforhold(ARBEIDSFORHOLD)
            .medDagsats(DAGSATS)
            .medDagsatsFraBg(DAGSATS_FRA_BG)
            .medUtbetalingssgrad(UTBETALINGSGRAD)
            .build();
        andelATD.addBeregningsresultatFeriepengerPrÅr(fraAndel(andelATD, BigDecimal.ONE, opptjeningsår));
        var andelATR = BeregningsresultatAndel.builder().medAktivitetStatus(AktivitetStatus.ATFL)
            .medBrukerErMottaker(false)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsforhold(ARBEIDSFORHOLD)
            .medDagsats(DAGSATS)
            .medDagsatsFraBg(DAGSATS_FRA_BG)
            .medUtbetalingssgrad(UTBETALINGSGRAD)
            .build();
        andelATR.addBeregningsresultatFeriepengerPrÅr(fraAndel(andelATR, BigDecimal.TEN, opptjeningsår));
        var andelFRI1 = BeregningsresultatAndel.builder().medAktivitetStatus(AktivitetStatus.ATFL)
            .medBrukerErMottaker(true)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsats(DAGSATS)
            .medDagsatsFraBg(DAGSATS_FRA_BG)
            .medUtbetalingssgrad(UTBETALINGSGRAD)
            .build();
        andelFRI1.addBeregningsresultatFeriepengerPrÅr(fraAndel(andelFRI1, BigDecimal.valueOf(2), opptjeningsår));
        var andelFRI2 = BeregningsresultatAndel.builder().medAktivitetStatus(AktivitetStatus.ATFL)
            .medBrukerErMottaker(true)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsats(DAGSATS)
            .medDagsatsFraBg(DAGSATS_FRA_BG)
            .medUtbetalingssgrad(UTBETALINGSGRAD)
            .build();
        andelFRI2.addBeregningsresultatFeriepengerPrÅr(fraAndel(andelFRI1, BigDecimal.valueOf(3), opptjeningsår));
        var andelSN = BeregningsresultatAndel.builder().medAktivitetStatus(AktivitetStatus.SN)
            .medBrukerErMottaker(true)
            .medInntektskategori(Inntektskategori.SJØMANN)
            .medDagsats(DAGSATS)
            .medDagsatsFraBg(DAGSATS_FRA_BG)
            .medUtbetalingssgrad(UTBETALINGSGRAD)
            .build();
        andelSN.addBeregningsresultatFeriepengerPrÅr(fraAndel(andelSN, BigDecimal.valueOf(25L), opptjeningsår));
        var periode = new BeregningsresultatPeriode(tidsperiode);
        periode.addBeregningsresultatAndel(andelATD);
        periode.addBeregningsresultatAndel(andelATR);
        periode.addBeregningsresultatAndel(andelFRI1);
        periode.addBeregningsresultatAndel(andelFRI2);
        periode.addBeregningsresultatAndel(andelSN);
        return periode;
    }

    private BeregningsresultatFeriepengerPrÅr fraAndel(BeregningsresultatAndel andel, BigDecimal årsbeløp, LocalDate opptjeningsår) {
        return BeregningsresultatFeriepengerPrÅr.builder().medÅrsbeløp(årsbeløp)
            .medOpptjeningÅr(opptjeningsår.with(MonthDay.of(Month.DECEMBER, 31)))
            .medBrukerErMottaker(andel.erBrukerMottaker())
            .medArbeidsforhold(andel.getArbeidsforhold())
            .medAktivitetStatus(andel.getAktivitetStatus())
            .build();
    }
}
