package no.nav.foreldrepenger.ytelse.beregning;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        andel.addBeregningsresultatFeriepengerPrÅr(fraAndel(andel, årsbeløp));
        var periode = new BeregningsresultatPeriode(PERIODE);
        periode.addBeregningsresultatAndel(andel);
        return periode;
    }

    @Test
    void skal_lage_feriepengeresultat_med_flere_andeler() {
        // Arrange
        var periode = lagPeriodeMedAndeler();
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
            .reduce(Beløp.ZERO, Beløp::adder).getVerdi()).isEqualByComparingTo(BigDecimal.TEN); // Arbeidsgiver direkte
    }

    private BeregningsresultatPeriode lagPeriodeMedAndeler() {
        var andelATD = BeregningsresultatAndel.builder().medAktivitetStatus(AktivitetStatus.ATFL)
            .medBrukerErMottaker(true)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsforhold(ARBEIDSFORHOLD)
            .medDagsats(DAGSATS)
            .medDagsatsFraBg(DAGSATS_FRA_BG)
            .medUtbetalingssgrad(UTBETALINGSGRAD)
            .build();
        andelATD.addBeregningsresultatFeriepengerPrÅr(fraAndel(andelATD, BigDecimal.ONE));
        var andelATR = BeregningsresultatAndel.builder().medAktivitetStatus(AktivitetStatus.ATFL)
            .medBrukerErMottaker(false)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsforhold(ARBEIDSFORHOLD)
            .medDagsats(DAGSATS)
            .medDagsatsFraBg(DAGSATS_FRA_BG)
            .medUtbetalingssgrad(UTBETALINGSGRAD)
            .build();
        andelATR.addBeregningsresultatFeriepengerPrÅr(fraAndel(andelATR, BigDecimal.TEN));
        var andelFRI1 = BeregningsresultatAndel.builder().medAktivitetStatus(AktivitetStatus.ATFL)
            .medBrukerErMottaker(true)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsats(DAGSATS)
            .medDagsatsFraBg(DAGSATS_FRA_BG)
            .medUtbetalingssgrad(UTBETALINGSGRAD)
            .build();
        andelFRI1.addBeregningsresultatFeriepengerPrÅr(fraAndel(andelFRI1, BigDecimal.valueOf(2)));
        var andelFRI2 = BeregningsresultatAndel.builder().medAktivitetStatus(AktivitetStatus.ATFL)
            .medBrukerErMottaker(true)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsats(DAGSATS)
            .medDagsatsFraBg(DAGSATS_FRA_BG)
            .medUtbetalingssgrad(UTBETALINGSGRAD)
            .build();
        andelFRI2.addBeregningsresultatFeriepengerPrÅr(fraAndel(andelFRI1, BigDecimal.valueOf(3)));
        var andelSN = BeregningsresultatAndel.builder().medAktivitetStatus(AktivitetStatus.SN)
            .medBrukerErMottaker(true)
            .medInntektskategori(Inntektskategori.SJØMANN)
            .medDagsats(DAGSATS)
            .medDagsatsFraBg(DAGSATS_FRA_BG)
            .medUtbetalingssgrad(UTBETALINGSGRAD)
            .build();
        andelSN.addBeregningsresultatFeriepengerPrÅr(fraAndel(andelSN, BigDecimal.valueOf(25L)));
        var periode = new BeregningsresultatPeriode(PERIODE);
        periode.addBeregningsresultatAndel(andelATD);
        periode.addBeregningsresultatAndel(andelATR);
        periode.addBeregningsresultatAndel(andelFRI1);
        periode.addBeregningsresultatAndel(andelFRI2);
        periode.addBeregningsresultatAndel(andelSN);
        return periode;
    }

    private BeregningsresultatFeriepengerPrÅr fraAndel(BeregningsresultatAndel andel, BigDecimal årsbeløp) {
        return BeregningsresultatFeriepengerPrÅr.builder().medÅrsbeløp(årsbeløp)
            .medOpptjeningÅr(LocalDate.now())
            .medBrukerErMottaker(andel.erBrukerMottaker())
            .medArbeidsforhold(andel.getArbeidsforhold())
            .medAktivitetStatus(andel.getAktivitetStatus())
            .build();
    }
}
